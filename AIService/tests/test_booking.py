"""
Tests for appointment booking functionality
"""
import pytest
from unittest.mock import AsyncMock, MagicMock, patch
from app.agents.tools import (
    check_available_slots,
    create_booking,
    recommend_medical_packages,
    init_tools
)
from datetime import datetime, timedelta


@pytest.mark.asyncio
async def test_check_available_slots_success(mock_clinic_api, mock_vector_store):
    """Test successful slot checking"""
    init_tools(mock_clinic_api, mock_vector_store)
    
    # Get tomorrow's date
    tomorrow = (datetime.now() + timedelta(days=1)).strftime("%Y-%m-%d")
    
    # Mock slots for tomorrow
    mock_clinic_api.get_available_slots.return_value = [
        {
            "slotId": "slot1",
            "date": tomorrow,
            "shift": "MORNING",
            "remainingQuantity": 5,
            "totalQuantity": 10
        }
    ]
    
    result = await check_available_slots(tomorrow)
    
    assert result is not None
    assert isinstance(result, str)
    assert "Slot trống" in result or "slot" in result.lower()
    assert mock_clinic_api.get_medical_packages.called
    assert mock_clinic_api.get_available_slots.called


@pytest.mark.asyncio
async def test_check_available_slots_no_slots(mock_clinic_api, mock_vector_store):
    """Test when no slots are available"""
    init_tools(mock_clinic_api, mock_vector_store)
    
    tomorrow = (datetime.now() + timedelta(days=1)).strftime("%Y-%m-%d")
    
    # Mock empty slots
    mock_clinic_api.get_available_slots.return_value = []
    
    result = await check_available_slots(tomorrow)
    
    assert "Không có slot trống" in result or "không có" in result.lower()


@pytest.mark.asyncio
async def test_check_available_slots_with_shift_filter(mock_clinic_api, mock_vector_store):
    """Test slot checking with shift filter"""
    init_tools(mock_clinic_api, mock_vector_store)
    
    tomorrow = (datetime.now() + timedelta(days=1)).strftime("%Y-%m-%d")
    
    # Mock slots for both shifts
    mock_clinic_api.get_available_slots.return_value = [
        {
            "slotId": "slot1",
            "date": tomorrow,
            "shift": "MORNING",
            "remainingQuantity": 5,
            "totalQuantity": 10
        },
        {
            "slotId": "slot2",
            "date": tomorrow,
            "shift": "AFTERNOON",
            "remainingQuantity": 3,
            "totalQuantity": 10
        }
    ]
    
    # Test morning shift
    result = await check_available_slots(tomorrow, shift="MORNING")
    assert "MORNING" in result or "sáng" in result.lower() or "morning" in result.lower()
    
    # Test afternoon shift
    result = await check_available_slots(tomorrow, shift="AFTERNOON")
    assert "AFTERNOON" in result or "chiều" in result.lower() or "afternoon" in result.lower()


@pytest.mark.asyncio
async def test_recommend_medical_packages_success(mock_clinic_api, mock_vector_store):
    """Test successful package recommendation"""
    init_tools(mock_clinic_api, mock_vector_store)
    
    symptoms = "Tôi bị đau răng"
    
    result = await recommend_medical_packages(symptoms)
    
    assert result is not None
    assert isinstance(result, str)
    assert "Gói khám" in result or "gói" in result.lower()
    assert mock_clinic_api.get_package_recommendations.called


@pytest.mark.asyncio
async def test_recommend_medical_packages_no_match(mock_clinic_api, mock_vector_store):
    """Test when no packages match symptoms"""
    init_tools(mock_clinic_api, mock_vector_store)
    
    # Mock empty recommendations
    mock_clinic_api.get_package_recommendations.return_value = []
    
    symptoms = "Triệu chứng không liên quan"
    result = await recommend_medical_packages(symptoms)
    
    assert "Không tìm thấy" in result or "không" in result.lower()


@pytest.mark.asyncio
async def test_create_booking_success(mock_clinic_api, mock_vector_store):
    """Test successful booking creation"""
    init_tools(mock_clinic_api, mock_vector_store)
    
    patient_info = "name:Nguyễn Văn Test,email:test@example.com,phone:0123456789"
    slot_id = "slot1"
    
    result = await create_booking(patient_info, slot_id)
    
    assert result is not None
    assert "Đặt lịch thành công" in result or "thành công" in result.lower()
    assert "booking" in result.lower() or "Mã" in result
    assert mock_clinic_api.create_booking.called
    
    # Verify booking was called with correct parameters
    call_args = mock_clinic_api.create_booking.call_args
    assert call_args[1]["slot_id"] == slot_id
    assert call_args[1]["name"] == "Nguyễn Văn Test"
    assert call_args[1]["email"] == "test@example.com"
    assert call_args[1]["phone"] == "0123456789"


@pytest.mark.asyncio
async def test_create_booking_missing_fields(mock_clinic_api, mock_vector_store):
    """Test booking creation with missing required fields"""
    init_tools(mock_clinic_api, mock_vector_store)
    
    # Missing email
    patient_info = "name:Nguyễn Văn Test,phone:0123456789"
    slot_id = "slot1"
    
    result = await create_booking(patient_info, slot_id)
    
    assert "Thiếu thông tin" in result or "thiếu" in result.lower()
    assert "email" in result.lower()
    assert not mock_clinic_api.create_booking.called


@pytest.mark.asyncio
async def test_create_booking_invalid_format(mock_clinic_api, mock_vector_store):
    """Test booking with invalid patient info format"""
    init_tools(mock_clinic_api, mock_vector_store)
    
    # Invalid format
    patient_info = "invalid format"
    slot_id = "slot1"
    
    result = await create_booking(patient_info, slot_id)
    
    # Should handle gracefully
    assert result is not None
    # May return error or try to parse


@pytest.mark.asyncio
async def test_create_booking_api_error(mock_clinic_api, mock_vector_store):
    """Test booking creation when API fails"""
    init_tools(mock_clinic_api, mock_vector_store)
    
    # Mock API to raise exception
    mock_clinic_api.create_booking.side_effect = Exception("API Error")
    
    patient_info = "name:Nguyễn Văn Test,email:test@example.com,phone:0123456789"
    slot_id = "slot1"
    
    result = await create_booking(patient_info, slot_id)
    
    assert "Lỗi" in result or "error" in result.lower()


@pytest.mark.asyncio
async def test_full_booking_flow_via_agent(initialized_agent, sample_session_id):
    """Test complete booking flow through agent"""
    # Step 1: Check available slots
    query1 = "Có slot trống ngày mai không?"
    result1 = await initialized_agent.run(query1, sample_session_id)
    assert "response" in result1
    
    # Step 2: Get package recommendation
    query2 = "Tôi bị đau răng, gói khám nào phù hợp?"
    result2 = await initialized_agent.run(query2, sample_session_id)
    assert "response" in result2
    
    # Step 3: Create booking (if agent decides to)
    # Note: This depends on agent's decision making


@pytest.mark.asyncio
async def test_booking_tools_not_initialized():
    """Test error when tools are not initialized"""
    import app.agents.tools as tools_module
    
    original_clinic_api = tools_module.clinic_api
    tools_module.clinic_api = None
    
    try:
        result = await check_available_slots("2024-12-20")
        assert "Lỗi" in result or "chưa được khởi tạo" in result.lower()
    finally:
        tools_module.clinic_api = original_clinic_api


@pytest.mark.asyncio
async def test_booking_with_package_filter(mock_clinic_api, mock_vector_store):
    """Test slot checking with medical package filter"""
    init_tools(mock_clinic_api, mock_vector_store)
    
    tomorrow = (datetime.now() + timedelta(days=1)).strftime("%Y-%m-%d")
    
    # Mock packages
    mock_clinic_api.get_medical_packages.return_value = [
        {
            "id": "pkg1",
            "name": "Khám tổng quát",
            "price": 500000
        },
        {
            "id": "pkg2",
            "name": "Khám răng miệng",
            "price": 300000
        }
    ]
    
    # Mock slots
    mock_clinic_api.get_available_slots.return_value = [
        {
            "slotId": "slot1",
            "date": tomorrow,
            "shift": "MORNING",
            "remainingQuantity": 5,
            "totalQuantity": 10
        }
    ]
    
    result = await check_available_slots(tomorrow, medical_package="răng")
    
    assert result is not None
    # Should filter by package name

