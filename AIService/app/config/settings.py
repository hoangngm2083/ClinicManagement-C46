from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    # API Configuration
    clinic_api_base_url: str = "http://api-gateway:8080"
    ai_service_port: int = 8000

    # OpenAI Configuration
    openai_api_key: str = "sk-fake-key-for-testing"  # Default for testing
    openai_model: str = "gpt-4o"
    openai_temperature: float = 0.1
    openai_embedding_model: str = "text-embedding-ada-002"

    # PostgreSQL Vector Database Configuration
    postgres_host: str = "postgres"
    postgres_port: int = 5432
    postgres_db: str = "vector_db"
    postgres_user: str = "booking"
    postgres_password: str = "booking"
    postgres_ssl_mode: str = "disable"

    # Vector Store Configuration
    vector_dimension: int = 1536
    vector_similarity_metric: str = "cosine"
    vector_table_name: str = "vector_embeddings"
    vector_index_type: str = "ivfflat"

    # Data Sync Configuration
    sync_doctors_interval_minutes: int = 15
    sync_slots_interval_minutes: int = 5
    sync_packages_interval_minutes: int = 30

    # Memory Configuration
    memory_max_tokens: int = 2000
    memory_window_size: int = 10
    memory_checkpoint_type: str = "MemorySaver"

    # LangGraph Configuration
    langgraph_agent_type: str = "ReAct"
    langgraph_max_iterations: int = 5
    langgraph_recursion_limit: int = 50

    # Retry Configuration
    retry_max_attempts: int = 3
    retry_backoff_multiplier: float = 1.0
    retry_max_delay: float = 10.0

    # Rate Limiting
    rate_limit_requests: int = 100
    rate_limit_window_seconds: int = 60

    # Logging Configuration
    log_level: str = "INFO"
    log_format: str = "json"

    # Feature Flags
    enable_vector_store: bool = True
    enable_langgraph: bool = True

    # Clinic Information
    clinic_name: str = "Phòng Khám Đa Khoa C46"
    clinic_working_hours: str = "Thứ 2-6: 8:00-17:00, Thứ 7-CN: 8:00-12:00"
    clinic_hotline: str = "1900-3497"
    clinic_email: str = "clinic.management.c46@gmail.com"
    clinic_address: str = "97 Man Thiện, phường Tăng Nhơn Phú, TP. Hồ Chí Minh"

    # Computed Properties
    @property
    def postgres_connection_string(self) -> str:
        return f"postgresql://{self.postgres_user}:{self.postgres_password}@{self.postgres_host}:{self.postgres_port}/{self.postgres_db}?sslmode={self.postgres_ssl_mode}"

    class Config:
        env_file = ".env"
        case_sensitive = False


settings = Settings()
