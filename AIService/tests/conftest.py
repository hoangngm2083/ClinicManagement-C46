"""
Pytest configuration and fixtures for AI Service tests
"""
import pytest
from unittest.mock import AsyncMock, MagicMock, Mock
from typing import AsyncGenerator
import asyncio

from app.services.clinic_api import ClinicAPIService
from app.rag.pgvector_store import PGVectorStore
from app.agents.langgraph_agent import LangGraphAgent, AgentManager


@pytest.fixture
def mock_clinic_api():
    """Mock ClinicAPIService"""
    mock = AsyncMock(spec=ClinicAPIService)
    
    # Mock doctor data
    mock.get_doctors = AsyncMock(return_value=[
        {
            "id": "1",
            "name": "Bác sĩ Nguyễn Văn A",
            "email": "doctor.a@clinic.com",
            "phone": "0123456789",
            "departmentName": "Nha khoa",
            "description": "Chuyên về răng miệng",
            "active": True
        },
        {
            "id": "2",
            "name": "Bác sĩ Trần Thị B",
            "email": "doctor.b@clinic.com",
            "phone": "0987654321",
            "departmentName": "Mắt",
            "description": "Chuyên về nhãn khoa",
            "active": True
        }
    ])
    
    # Mock medical packages
    mock.get_medical_packages = AsyncMock(return_value=[
        {
            "id": "pkg1",
            "name": "Khám tổng quát cơ bản",
            "price": 500000,
            "description": "Gói khám tổng quát cơ bản cho người lớn"
        },
        {
            "id": "pkg2",
            "name": "Khám răng miệng",
            "price": 300000,
            "description": "Gói khám răng miệng cơ bản"
        }
    ])
    
    # Mock available slots
    mock.get_available_slots = AsyncMock(return_value=[
        {
            "slotId": "slot1",
            "date": "2024-12-20",
            "shift": "MORNING",
            "remainingQuantity": 5,
            "totalQuantity": 10
        },
        {
            "slotId": "slot2",
            "date": "2024-12-20",
            "shift": "AFTERNOON",
            "remainingQuantity": 3,
            "totalQuantity": 10
        }
    ])
    
    # Mock create booking
    mock.create_booking = AsyncMock(return_value="booking123")
    
    # Mock package recommendations
    mock.get_package_recommendations = AsyncMock(return_value=[
        {
            "id": "pkg2",
            "name": "Khám răng miệng",
            "price": 300000,
            "description": "Gói khám răng miệng cơ bản"
        }
    ])
    
    # Mock doctor schedule
    mock.get_doctor_schedule = AsyncMock(return_value=[
        {
            "name": "Bác sĩ Nguyễn Văn A",
            "departmentName": "Nha khoa"
        }
    ])
    
    return mock


@pytest.fixture
def mock_vector_store():
    """Mock PGVectorStore"""
    mock = MagicMock(spec=PGVectorStore)
    
    # Mock similarity search results
    mock.similarity_search = MagicMock(return_value=[
        (
            {
                "title": "Giờ hoạt động phòng khám",
                "content": "Phòng khám hoạt động từ 7:00 đến 17:00 từ thứ 2 đến thứ 6",
                "type": "clinic_info"
            },
            0.95
        ),
        (
            {
                "question": "Làm thế nào để đặt lịch?",
                "answer": "Bạn có thể đặt lịch qua hotline hoặc website",
                "type": "faq"
            },
            0.88
        )
    ])
    
    mock.health_check = MagicMock(return_value=True)
    mock.db_available = True
    
    return mock


@pytest.fixture
async def initialized_agent(mock_clinic_api, mock_vector_store):
    """Create and initialize LangGraphAgent with mocked services"""
    from app.agents.tools import init_tools
    
    # Initialize tools with mocked services
    init_tools(mock_clinic_api, mock_vector_store)
    
    # Create agent
    agent = LangGraphAgent()
    
    # Initialize agent
    await agent.initialize(mock_clinic_api, mock_vector_store)
    
    return agent


@pytest.fixture
async def agent_manager(mock_clinic_api, mock_vector_store):
    """Create and initialize AgentManager"""
    manager = AgentManager()
    await manager.initialize_default_agent(mock_clinic_api, mock_vector_store)
    return manager


@pytest.fixture
def sample_session_id():
    """Sample session ID for testing"""
    return "test_session_123"


@pytest.fixture
def sample_patient_info():
    """Sample patient information"""
    return {
        "name": "Nguyễn Văn Test",
        "email": "test@example.com",
        "phone": "0123456789"
    }

