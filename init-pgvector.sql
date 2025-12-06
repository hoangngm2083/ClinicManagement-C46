-- Initialize pgvector extension and vector database schema
-- This script runs after init-db.sh creates vector_db

-- Connect to vector_db first
\c vector_db

-- Enable pgvector extension in vector_db
CREATE EXTENSION IF NOT EXISTS vector;

-- Create vector embeddings table
CREATE TABLE IF NOT EXISTS vector_embeddings (
    id SERIAL PRIMARY KEY,
    collection_name VARCHAR(50) NOT NULL,
    document_id VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    metadata JSONB,
    embedding VECTOR(1536), -- OpenAI ada-002 dimension
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(collection_name, document_id)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_vector_embeddings_collection
ON vector_embeddings (collection_name);

CREATE INDEX IF NOT EXISTS idx_vector_embeddings_metadata
ON vector_embeddings USING gin (metadata);

-- Create vector index based on configuration (IVFFlat for better recall)
CREATE INDEX IF NOT EXISTS idx_vector_embeddings_embedding_ivfflat
ON vector_embeddings
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

-- Grant permissions
GRANT ALL PRIVILEGES ON TABLE vector_embeddings TO booking;
GRANT USAGE ON SEQUENCE vector_embeddings_id_seq TO booking;
