import chromadb
from chromadb.config import Settings
from langchain_community.vectorstores import Chroma
from langchain_openai import OpenAIEmbeddings
from typing import List, Dict, Any, Optional
import logging
from ..config.settings import settings

logger = logging.getLogger(__name__)


class VectorStore:
    def __init__(self):
        self.embedding = OpenAIEmbeddings(
            model=settings.openai_embedding_model,
            openai_api_key=settings.openai_api_key
        )

        # Initialize ChromaDB client
        if settings.chroma_host and settings.chroma_port:
            # Remote ChromaDB
            self.client = chromadb.HttpClient(
                host=settings.chroma_host,
                port=settings.chroma_port
            )
        else:
            # Local ChromaDB
            self.client = chromadb.PersistentClient(
                path=settings.chroma_db_path,
                settings=Settings(anonymized_telemetry=False)
            )

        # Initialize collections
        self._init_collections()

        logger.info("VectorStore initialized successfully")

    def _init_collections(self):
        """Initialize vector collections"""
        try:
            self.doctor_collection = self.client.get_or_create_collection(
                name="doctors",
                metadata={"description": "Doctor information and schedules"}
            )

            self.package_collection = self.client.get_or_create_collection(
                name="medical_packages",
                metadata={"description": "Medical package information"}
            )

            self.process_collection = self.client.get_or_create_collection(
                name="clinic_processes",
                metadata={"description": "Clinic procedures and processes"}
            )

            self.faq_collection = self.client.get_or_create_collection(
                name="faq",
                metadata={"description": "Frequently asked questions"}
            )

            logger.info("Collections initialized")
        except Exception as e:
            logger.error(f"Error initializing collections: {e}")
            raise

    def health_check(self) -> bool:
        """Check if vector store is healthy"""
        try:
            self.client.heartbeat()
            return True
        except Exception as e:
            logger.error(f"Vector store health check failed: {e}")
            return False

    async def add_doctor_documents(self, doctors: List[Dict[str, Any]]):
        """Add doctor documents to vector store"""
        try:
            ids = []
            documents = []
            metadatas = []

            for doctor in doctors:
                doc_id = f"doctor_{doctor['id']}"
                content = f"""
                Bác sĩ {doctor.get('name', '')}.
                Email: {doctor.get('email', '')}.
                Điện thoại: {doctor.get('phone', '')}.
                Chuyên khoa: {doctor.get('departmentName', 'Chưa xác định')}.
                Mô tả: {doctor.get('description', 'Không có mô tả')}.
                Trạng thái: {'Đang hoạt động' if doctor.get('active', True) else 'Tạm nghỉ'}.
                """

                ids.append(doc_id)
                documents.append(content.strip())
                metadatas.append({
                    "type": "doctor",
                    "id": doctor["id"],
                    "name": doctor.get("name", ""),
                    "department": doctor.get("departmentName", ""),
                    "email": doctor.get("email", ""),
                    "phone": doctor.get("phone", ""),
                    "active": doctor.get("active", True)
                })

            if ids:
                self.doctor_collection.add(
                    documents=documents,
                    metadatas=metadatas,
                    ids=ids
                )
                logger.info(f"Added {len(ids)} doctor documents")

        except Exception as e:
            logger.error(f"Error adding doctor documents: {e}")
            raise

    async def add_package_documents(self, packages: List[Dict[str, Any]]):
        """Add medical package documents to vector store"""
        try:
            ids = []
            documents = []
            metadatas = []

            for package in packages:
                doc_id = f"package_{package['id']}"
                content = f"""
                Gói khám: {package.get('name', '')}.
                Mô tả: {package.get('description', 'Không có mô tả')}.
                Giá: {package.get('price', 0)} VND.
                """

                ids.append(doc_id)
                documents.append(content.strip())
                metadatas.append({
                    "type": "medical_package",
                    "id": package["id"],
                    "name": package.get("name", ""),
                    "price": package.get("price", 0),
                    "description": package.get("description", "")
                })

            if ids:
                self.package_collection.add(
                    documents=documents,
                    metadatas=metadatas,
                    ids=ids
                )
                logger.info(f"Added {len(ids)} package documents")

        except Exception as e:
            logger.error(f"Error adding package documents: {e}")
            raise

    async def add_process_documents(self, processes: List[Dict[str, Any]]):
        """Add clinic process documents to vector store"""
        try:
            ids = []
            documents = []
            metadatas = []

            for process in processes:
                doc_id = f"process_{process['id']}"
                content = process.get('content', '')

                ids.append(doc_id)
                documents.append(content)
                metadatas.append({
                    "type": "process",
                    "id": process["id"],
                    "title": process.get("title", "")
                })

            if ids:
                self.process_collection.add(
                    documents=documents,
                    metadatas=metadatas,
                    ids=ids
                )
                logger.info(f"Added {len(ids)} process documents")

        except Exception as e:
            logger.error(f"Error adding process documents: {e}")
            raise

    async def add_faq_documents(self, faqs: List[Dict[str, Any]]):
        """Add FAQ documents to vector store"""
        try:
            ids = []
            documents = []
            metadatas = []

            for faq in faqs:
                doc_id = f"faq_{faq['id']}"
                content = f"""
                Câu hỏi: {faq.get('question', '')}
                Trả lời: {faq.get('answer', '')}
                """

                ids.append(doc_id)
                documents.append(content.strip())
                metadatas.append({
                    "type": "faq",
                    "id": faq["id"],
                    "question": faq.get("question", ""),
                    "category": faq.get("category", "")
                })

            if ids:
                self.faq_collection.add(
                    documents=documents,
                    metadatas=metadatas,
                    ids=ids
                )
                logger.info(f"Added {len(ids)} FAQ documents")

        except Exception as e:
            logger.error(f"Error adding FAQ documents: {e}")
            raise

    def search_doctors(self, query: str, n_results: int = 5) -> Dict[str, Any]:
        """Search for doctors"""
        try:
            results = self.doctor_collection.query(
                query_texts=[query],
                n_results=n_results
            )
            return results
        except Exception as e:
            logger.error(f"Error searching doctors: {e}")
            return {"documents": [], "metadatas": []}

    def search_packages(self, query: str, n_results: int = 5) -> Dict[str, Any]:
        """Search for medical packages"""
        try:
            results = self.package_collection.query(
                query_texts=[query],
                n_results=n_results
            )
            return results
        except Exception as e:
            logger.error(f"Error searching packages: {e}")
            return {"documents": [], "metadatas": []}

    def search_processes(self, query: str, n_results: int = 3) -> Dict[str, Any]:
        """Search for clinic processes"""
        try:
            results = self.process_collection.query(
                query_texts=[query],
                n_results=n_results
            )
            return results
        except Exception as e:
            logger.error(f"Error searching processes: {e}")
            return {"documents": [], "metadatas": []}

    def search_faq(self, query: str, n_results: int = 3) -> Dict[str, Any]:
        """Search FAQ"""
        try:
            results = self.faq_collection.query(
                query_texts=[query],
                n_results=n_results
            )
            return results
        except Exception as e:
            logger.error(f"Error searching FAQ: {e}")
            return {"documents": [], "metadatas": []}

    async def update_doctor_documents(self, doctors: List[Dict[str, Any]]):
        """Update doctor documents (replace all)"""
        try:
            # Clear existing documents
            self.doctor_collection.delete()
            # Add new documents
            await self.add_doctor_documents(doctors)
            logger.info("Doctor documents updated")
        except Exception as e:
            logger.error(f"Error updating doctor documents: {e}")
            raise

    async def update_package_documents(self, packages: List[Dict[str, Any]]):
        """Update package documents (replace all)"""
        try:
            self.package_collection.delete()
            await self.add_package_documents(packages)
            logger.info("Package documents updated")
        except Exception as e:
            logger.error(f"Error updating package documents: {e}")
            raise

    def get_langchain_vectorstore(self, collection_name: str) -> Chroma:
        """Get LangChain Chroma vectorstore for a collection"""
        return Chroma(
            client=self.client,
            collection_name=collection_name,
            embedding_function=self.embedding
        )
