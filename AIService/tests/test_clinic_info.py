"""
Tests for clinic information functionality
"""
import pytest
from unittest.mock import AsyncMock, MagicMock, patch
from app.agents.tools import get_clinic_info, init_tools


@pytest.mark.asyncio
async def test_get_clinic_info_success(mock_clinic_api, mock_vector_store):
    """Test successful clinic information retrieval"""
    # Initialize tools
    init_tools(mock_clinic_api, mock_vector_store)
    
    # Test query
    query = "Giờ hoạt động của phòng khám là gì?"
    result = await get_clinic_info(query)
    
    # Assertions
    assert result is not None
    assert isinstance(result, str)
    assert len(result) > 0
    assert "Giờ hoạt động" in result or "7:00" in result or "17:00" in result
    
    # Verify vector store was called
    assert mock_vector_store.similarity_search.called
    calls = mock_vector_store.similarity_search.call_args_list
    assert len(calls) >= 2  # Should search both clinic_processes and faq


@pytest.mark.asyncio
async def test_get_clinic_info_no_results(mock_clinic_api, mock_vector_store):
    """Test clinic info when no results found"""
    # Mock empty results
    mock_vector_store.similarity_search.return_value = []
    
    init_tools(mock_clinic_api, mock_vector_store)
    
    query = "Câu hỏi không liên quan"
    result = await get_clinic_info(query)
    
    # Should return fallback information
    assert result is not None
    assert "Giờ hoạt động" in result or "Hotline" in result or "Email" in result


@pytest.mark.asyncio
async def test_get_clinic_info_processes_search(mock_clinic_api, mock_vector_store):
    """Test that clinic processes are searched"""
    init_tools(mock_clinic_api, mock_vector_store)
    
    query = "Quy trình khám bệnh như thế nào?"
    
    # Set up mock to return process results
    mock_vector_store.similarity_search.side_effect = [
        [  # First call for clinic_processes
            (
                {
                    "title": "Quy trình khám bệnh",
                    "content": "Bước 1: Đăng ký, Bước 2: Khám, Bước 3: Thanh toán"
                },
                0.92
            )
        ],
        []  # Second call for faq (empty)
    ]
    
    result = await get_clinic_info(query)
    
    assert "Quy trình" in result or "Bước" in result
    assert mock_vector_store.similarity_search.call_count >= 1


@pytest.mark.asyncio
async def test_get_clinic_info_faq_search(mock_clinic_api, mock_vector_store):
    """Test that FAQ is searched"""
    init_tools(mock_clinic_api, mock_vector_store)
    
    query = "Làm sao để đặt lịch?"
    
    # Set up mock to return FAQ results
    mock_vector_store.similarity_search.side_effect = [
        [],  # First call for clinic_processes (empty)
        [  # Second call for faq
            (
                {
                    "question": "Làm thế nào để đặt lịch?",
                    "answer": "Bạn có thể đặt lịch qua hotline 1900-xxxx hoặc website"
                },
                0.90
            )
        ]
    ]
    
    result = await get_clinic_info(query)
    
    assert "đặt lịch" in result.lower() or "hotline" in result.lower()
    assert mock_vector_store.similarity_search.call_count >= 2


@pytest.mark.asyncio
async def test_get_clinic_info_vector_store_error(mock_clinic_api, mock_vector_store):
    """Test error handling when vector store fails"""
    # Mock vector store to raise exception
    mock_vector_store.similarity_search.side_effect = Exception("Database error")
    
    init_tools(mock_clinic_api, mock_vector_store)
    
    query = "Thông tin phòng khám"
    result = await get_clinic_info(query)
    
    # Should return error message
    assert "Lỗi" in result or "error" in result.lower()


@pytest.mark.asyncio
async def test_get_clinic_info_tools_not_initialized():
    """Test error when tools are not initialized"""
    # Temporarily set to None
    import app.agents.tools as tools_module
    original_clinic_api = tools_module.clinic_api
    original_vector_store = tools_module.vector_store
    
    tools_module.clinic_api = None
    tools_module.vector_store = None
    
    try:
        result = await get_clinic_info("test query")
        assert "Lỗi" in result or "chưa được khởi tạo" in result.lower()
    finally:
        # Restore
        tools_module.clinic_api = original_clinic_api
        tools_module.vector_store = original_vector_store


@pytest.mark.asyncio
async def test_clinic_info_via_agent(initialized_agent, sample_session_id):
    """Test clinic info through full agent"""
    # Test various clinic info queries
    test_queries = [
        "Giờ hoạt động của phòng khám?",
        "Phòng khám mở cửa lúc mấy giờ?",
        "Làm sao để liên hệ phòng khám?",
        "Quy trình khám bệnh như thế nào?"
    ]
    
    for query in test_queries:
        result = await initialized_agent.run(query, sample_session_id)
        
        assert result is not None
        assert "response" in result
        assert isinstance(result["response"], str)
        assert len(result["response"]) > 0
        assert result["session_id"] == sample_session_id


@pytest.mark.asyncio
async def test_clinic_info_multiple_queries_same_session(initialized_agent, sample_session_id):
    """Test multiple clinic info queries in same session (memory test)"""
    queries = [
        "Giờ hoạt động của phòng khám?",
        "Phòng khám có mở cửa vào cuối tuần không?"
    ]
    
    responses = []
    for query in queries:
        result = await initialized_agent.run(query, sample_session_id)
        responses.append(result)
        assert result["session_id"] == sample_session_id
    
    # Both should succeed
    assert len(responses) == 2
    assert all("response" in r for r in responses)

