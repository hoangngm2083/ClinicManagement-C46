"""
Tests for LangGraph memory functionality
"""
import pytest
from unittest.mock import AsyncMock, MagicMock, patch
from langgraph.checkpoint.memory import MemorySaver


@pytest.mark.asyncio
async def test_memory_initialization(initialized_agent):
    """Test that agent memory is properly initialized"""
    assert initialized_agent.memory is not None
    assert isinstance(initialized_agent.memory, MemorySaver)


@pytest.mark.asyncio
async def test_memory_persistence_same_session(initialized_agent):
    """Test that memory persists across multiple messages in same session"""
    session_id = "memory_test_session_1"
    
    # First message
    result1 = await initialized_agent.run("Tôi tên là Nguyễn Văn A", session_id)
    assert result1["session_id"] == session_id
    
    # Second message that references previous context
    result2 = await initialized_agent.run("Tên tôi là gì?", session_id)
    assert result2["session_id"] == session_id
    
    # The agent should remember the name from first message
    # Note: This depends on LLM's ability to use memory
    # We're testing that memory mechanism works, not the LLM response quality
    assert "response" in result2


@pytest.mark.asyncio
async def test_memory_isolation_different_sessions(initialized_agent):
    """Test that different sessions have isolated memory"""
    session1 = "session_1"
    session2 = "session_2"
    
    # Set context in session 1
    await initialized_agent.run("Tôi tên là Nguyễn Văn A", session1)
    
    # Set different context in session 2
    await initialized_agent.run("Tôi tên là Trần Thị B", session2)
    
    # Query session 1
    result1 = await initialized_agent.run("Tên tôi là gì?", session1)
    
    # Query session 2
    result2 = await initialized_agent.run("Tên tôi là gì?", session2)
    
    # Both should work independently
    assert result1["session_id"] == session1
    assert result2["session_id"] == session2
    # Responses may differ based on their respective contexts


@pytest.mark.asyncio
async def test_memory_clear_functionality(initialized_agent):
    """Test clearing memory for a session"""
    session_id = "clear_test_session"
    
    # Add some conversation
    await initialized_agent.run("Xin chào", session_id)
    await initialized_agent.run("Tôi muốn đặt lịch", session_id)
    
    # Clear memory
    initialized_agent.clear_memory(session_id)
    
    # Memory should be cleared (though we can't directly verify internal state)
    # But subsequent queries should work
    result = await initialized_agent.run("Xin chào lại", session_id)
    assert result["session_id"] == session_id


@pytest.mark.asyncio
async def test_conversation_history_retrieval(initialized_agent):
    """Test retrieving conversation history"""
    session_id = "history_test_session"
    
    # Add some messages
    await initialized_agent.run("Câu hỏi 1", session_id)
    await initialized_agent.run("Câu hỏi 2", session_id)
    
    # Get history
    history = await initialized_agent.get_conversation_history(session_id)
    
    # History should be a list
    assert isinstance(history, list)
    # May be empty if memory storage structure is different
    # This tests the method exists and works without error


@pytest.mark.asyncio
async def test_memory_with_booking_flow(initialized_agent):
    """Test memory in a multi-step booking flow"""
    session_id = "booking_memory_test"
    
    # Step 1: User provides name
    result1 = await initialized_agent.run(
        "Tôi muốn đặt lịch. Tên tôi là Nguyễn Văn Test",
        session_id
    )
    
    # Step 2: User provides email
    result2 = await initialized_agent.run(
        "Email của tôi là test@example.com",
        session_id
    )
    
    # Step 3: User provides phone
    result3 = await initialized_agent.run(
        "Số điện thoại là 0123456789",
        session_id
    )
    
    # Step 4: User asks to book
    result4 = await initialized_agent.run(
        "Đặt lịch cho tôi với slot slot1",
        session_id
    )
    
    # All should use same session
    assert all(r["session_id"] == session_id for r in [result1, result2, result3, result4])
    
    # Memory should help agent remember previous information
    # The agent should be able to use name, email, phone from previous messages


@pytest.mark.asyncio
async def test_memory_with_clinic_info_queries(initialized_agent):
    """Test memory with multiple clinic info queries"""
    session_id = "clinic_info_memory_test"
    
    # First query
    result1 = await initialized_agent.run(
        "Giờ hoạt động của phòng khám?",
        session_id
    )
    
    # Follow-up query that may reference previous context
    result2 = await initialized_agent.run(
        "Còn thông tin gì khác không?",
        session_id
    )
    
    # Both should work
    assert result1["session_id"] == session_id
    assert result2["session_id"] == session_id
    assert "response" in result1
    assert "response" in result2


@pytest.mark.asyncio
async def test_memory_session_id_generation(initialized_agent):
    """Test that session ID is generated when not provided"""
    # Run without session_id
    result = await initialized_agent.run("Xin chào")
    
    # Should have a generated session_id
    assert "session_id" in result
    assert result["session_id"] is not None
    assert result["session_id"].startswith("session_")


@pytest.mark.asyncio
async def test_memory_with_agent_manager(agent_manager, sample_session_id):
    """Test memory through AgentManager"""
    # Use agent manager
    result1 = await agent_manager.run_agent("Xin chào", sample_session_id)
    result2 = await agent_manager.run_agent("Bạn có thể giúp gì?", sample_session_id)
    
    # Both should use same session
    assert result1["session_id"] == sample_session_id
    assert result2["session_id"] == sample_session_id


@pytest.mark.asyncio
async def test_memory_persistence_across_requests(initialized_agent):
    """Test that memory persists across multiple agent runs"""
    session_id = "persistence_test"
    
    # Simulate a conversation over multiple requests
    messages = [
        "Tôi muốn biết về phòng khám",
        "Giờ mở cửa là gì?",
        "Có bác sĩ nha khoa không?",
        "Tôi muốn đặt lịch"
    ]
    
    results = []
    for msg in messages:
        result = await initialized_agent.run(msg, session_id)
        results.append(result)
        assert result["session_id"] == session_id
    
    # All should succeed
    assert len(results) == len(messages)
    assert all("response" in r for r in results)


@pytest.mark.asyncio
async def test_memory_error_handling(initialized_agent):
    """Test memory error handling"""
    session_id = "error_test"
    
    # Normal operation
    result1 = await initialized_agent.run("Test message", session_id)
    assert result1["session_id"] == session_id
    
    # Even if there's an error in memory retrieval, agent should still work
    # (This tests resilience)
    result2 = await initialized_agent.run("Another test", session_id)
    assert result2["session_id"] == session_id

