import logging
from typing import List, Dict, Any, Optional, Tuple
import psycopg2
from psycopg2.extras import RealDictCursor
from pgvector.psycopg2 import register_vector
import numpy as np
from langchain_openai import OpenAIEmbeddings
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type
from ..config.settings import settings

logger = logging.getLogger(__name__)


class PGVectorStore:
    """PostgreSQL Vector Store using pgvector extension"""

    def __init__(self):
        self.embedding = OpenAIEmbeddings(
            model=settings.openai_embedding_model,
            openai_api_key=settings.openai_api_key
        )

        self.connection_params = {
            'host': settings.postgres_host,
            'port': settings.postgres_port,
            'database': settings.postgres_db,
            'user': settings.postgres_user,
            'password': settings.postgres_password,
            'sslmode': settings.postgres_ssl_mode
        }

        # Initialize database and tables
        self.db_available = False
        try:
            self._init_database()
            self.db_available = True
            logger.info("PGVectorStore initialized successfully")
        except Exception as e:
            logger.warning(f"PGVectorStore initialization failed, using fallback mode: {e}")
            self.db_available = False

    def _init_database(self):
        """Initialize database tables and extensions"""
        with self._get_connection() as conn:
            with conn.cursor() as cursor:
                # Enable pgvector extension
                cursor.execute("CREATE EXTENSION IF NOT EXISTS vector;")

                # Create vector embeddings table
                cursor.execute(f"""
                    CREATE TABLE IF NOT EXISTS {settings.vector_table_name} (
                        id SERIAL PRIMARY KEY,
                        collection_name VARCHAR(50) NOT NULL,
                        document_id VARCHAR(100) NOT NULL,
                        content TEXT NOT NULL,
                        metadata JSONB,
                        embedding VECTOR({settings.vector_dimension}),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(collection_name, document_id)
                    );
                """)

                # Create indexes for performance
                cursor.execute(f"""
                    CREATE INDEX IF NOT EXISTS idx_{settings.vector_table_name}_collection
                    ON {settings.vector_table_name} (collection_name);
                """)

                cursor.execute(f"""
                    CREATE INDEX IF NOT EXISTS idx_{settings.vector_table_name}_metadata
                    ON {settings.vector_table_name} USING gin (metadata);
                """)

                # Create vector index based on configuration
                if settings.vector_index_type == "ivfflat":
                    cursor.execute(f"""
                        CREATE INDEX IF NOT EXISTS idx_{settings.vector_table_name}_embedding_ivfflat
                        ON {settings.vector_table_name}
                        USING ivfflat (embedding vector_cosine_ops)
                        WITH (lists = 100);
                    """)
                elif settings.vector_index_type == "hnsw":
                    cursor.execute(f"""
                        CREATE INDEX IF NOT EXISTS idx_{settings.vector_table_name}_embedding_hnsw
                        ON {settings.vector_table_name}
                        USING hnsw (embedding vector_cosine_ops)
                        WITH (m = 16, ef_construction = 64);
                    """)

            conn.commit()
            logger.info("Database tables and indexes initialized")

    def _get_connection(self):
        """Get database connection with vector support"""
        conn = psycopg2.connect(**self.connection_params)
        register_vector(conn)
        return conn

    @retry(
        stop=stop_after_attempt(settings.retry_max_attempts),
        wait=wait_exponential(multiplier=settings.retry_backoff_multiplier, max=settings.retry_max_delay),
        retry=retry_if_exception_type(psycopg2.Error)
    )
    def add_documents(self, collection_name: str, documents: List[str],
                     metadatas: List[Dict[str, Any]], ids: List[str]) -> None:
        """Add documents to vector store"""
        try:
            # Generate embeddings
            embeddings = self.embedding.embed_documents(documents)

            with self._get_connection() as conn:
                with conn.cursor() as cursor:
                    # Prepare data for bulk insert
                    data = []
                    for doc, metadata, doc_id, embedding in zip(documents, metadatas, ids, embeddings):
                        data.append((
                            collection_name,
                            doc_id,
                            doc,
                            psycopg2.extras.Json(metadata),
                            np.array(embedding)
                        ))

                    # Bulk insert with ON CONFLICT UPDATE
                    cursor.executemany(f"""
                        INSERT INTO {settings.vector_table_name}
                        (collection_name, document_id, content, metadata, embedding)
                        VALUES (%s, %s, %s, %s, %s)
                        ON CONFLICT (collection_name, document_id)
                        DO UPDATE SET
                            content = EXCLUDED.content,
                            metadata = EXCLUDED.metadata,
                            embedding = EXCLUDED.embedding;
                    """, data)

                conn.commit()
                logger.info(f"Added {len(documents)} documents to collection '{collection_name}'")

        except Exception as e:
            logger.error(f"Error adding documents to {collection_name}: {e}")
            raise

    @retry(
        stop=stop_after_attempt(settings.retry_max_attempts),
        wait=wait_exponential(multiplier=settings.retry_backoff_multiplier, max=settings.retry_max_delay),
        retry=retry_if_exception_type(psycopg2.Error)
    )
    def similarity_search(self, collection_name: str, query: str,
                        n_results: int = 5) -> List[Tuple[Dict[str, Any], float]]:
        """Perform similarity search"""
        if not self.db_available:
            logger.info(f"Vector store not available, returning empty results for {collection_name}")
            return []

        try:
            # Generate query embedding
            query_embedding = self.embedding.embed_query(query)

            with self._get_connection() as conn:
                with conn.cursor(cursor_factory=RealDictCursor) as cursor:
                    # Perform vector similarity search
                    if settings.vector_similarity_metric == "cosine":
                        similarity_func = "1 - (embedding <=> %s::vector)"
                    elif settings.vector_similarity_metric == "l2":
                        similarity_func = "embedding <-> %s::vector"
                    elif settings.vector_similarity_metric == "ip":
                        similarity_func = "embedding <#> %s::vector"
                    else:
                        similarity_func = "1 - (embedding <=> %s::vector)"

                    cursor.execute(f"""
                        SELECT
                            content,
                            metadata,
                            {similarity_func} as similarity_score
                        FROM {settings.vector_table_name}
                        WHERE collection_name = %s
                        ORDER BY {similarity_func} DESC
                        LIMIT %s;
                    """, (np.array(query_embedding), collection_name, n_results))

                    results = cursor.fetchall()

                    # Format results
                    formatted_results = []
                    for row in results:
                        formatted_results.append((dict(row), row['similarity_score']))

                    return formatted_results

        except Exception as e:
            logger.error(f"Error performing similarity search in {collection_name}: {e}")
            return []

    def delete_collection(self, collection_name: str) -> None:
        """Delete all documents in a collection"""
        try:
            with self._get_connection() as conn:
                with conn.cursor() as cursor:
                    cursor.execute(f"""
                        DELETE FROM {settings.vector_table_name}
                        WHERE collection_name = %s;
                    """, (collection_name,))

                conn.commit()
                logger.info(f"Deleted collection '{collection_name}'")

        except Exception as e:
            logger.error(f"Error deleting collection {collection_name}: {e}")
            raise

    def collection_exists(self, collection_name: str) -> bool:
        """Check if collection exists"""
        try:
            with self._get_connection() as conn:
                with conn.cursor() as cursor:
                    cursor.execute(f"""
                        SELECT COUNT(*) FROM {settings.vector_table_name}
                        WHERE collection_name = %s LIMIT 1;
                    """, (collection_name,))

                    count = cursor.fetchone()[0]
                    return count > 0

        except Exception as e:
            logger.error(f"Error checking collection existence: {e}")
            return False

    def get_collection_stats(self, collection_name: str) -> Dict[str, Any]:
        """Get collection statistics"""
        try:
            with self._get_connection() as conn:
                with conn.cursor(cursor_factory=RealDictCursor) as cursor:
                    cursor.execute(f"""
                        SELECT
                            COUNT(*) as total_documents,
                            AVG(ARRAY_LENGTH(embedding::float[], 1)) as avg_embedding_dim,
                            MIN(created_at) as oldest_document,
                            MAX(created_at) as newest_document
                        FROM {settings.vector_table_name}
                        WHERE collection_name = %s;
                    """, (collection_name,))

                    stats = cursor.fetchone()
                    return dict(stats) if stats else {}

        except Exception as e:
            logger.error(f"Error getting collection stats: {e}")
            return {}

    def health_check(self) -> bool:
        """Check if vector store is healthy"""
        if not self.db_available:
            return False
        try:
            with self._get_connection() as conn:
                with conn.cursor() as cursor:
                    cursor.execute("SELECT 1;")
                    return True
        except Exception as e:
            logger.error(f"Vector store health check failed: {e}")
            return False
