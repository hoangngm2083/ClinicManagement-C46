# Hướng Dẫn Test AI Service

Tài liệu này mô tả cách chạy và sử dụng bộ test đầy đủ cho AI Service.

## 📋 Tổng Quan

Bộ test bao gồm:

1. **Test Clinic Information** - Test chức năng cung cấp thông tin phòng khám
2. **Test Booking** - Test chức năng đặt lịch cho bệnh nhân
3. **Test Memory** - Test LangGraph memory hoạt động
4. **Integration Tests** - Test kết hợp với các service khác

## 🚀 Cách Chạy Tests

### 1. Chạy tất cả tests

```bash
cd AIService
pytest tests/
```

### 2. Sử dụng script bash

```bash
cd AIService
./tests/run_all_tests.sh
```

Với coverage:
```bash
./tests/run_all_tests.sh --coverage
```

### 3. Sử dụng script Python

```bash
cd AIService
python tests/run_tests.py
```

Với các options:
```bash
python tests/run_tests.py --coverage --verbose
python tests/run_tests.py --file test_clinic_info.py
python tests/run_tests.py --test test_get_clinic_info_success
```

### 4. Chạy từng nhóm tests

```bash
# Test clinic info
pytest tests/test_clinic_info.py -v

# Test booking
pytest tests/test_booking.py -v

# Test memory
pytest tests/test_memory.py -v

# Test integration
pytest tests/test_integration.py -v
```

## 📝 Chi Tiết Test Cases

### Test Clinic Information (`test_clinic_info.py`)

**Mục đích**: Test chức năng cung cấp thông tin phòng khám

**Test cases**:
- ✅ `test_get_clinic_info_success` - Tìm kiếm thông tin thành công
- ✅ `test_get_clinic_info_no_results` - Xử lý khi không có kết quả
- ✅ `test_get_clinic_info_processes_search` - Tìm kiếm trong clinic processes
- ✅ `test_get_clinic_info_faq_search` - Tìm kiếm trong FAQ
- ✅ `test_get_clinic_info_vector_store_error` - Error handling
- ✅ `test_get_clinic_info_tools_not_initialized` - Tools chưa khởi tạo
- ✅ `test_clinic_info_via_agent` - Test qua agent
- ✅ `test_clinic_info_multiple_queries_same_session` - Multiple queries cùng session

**Chạy**:
```bash
pytest tests/test_clinic_info.py -v
```

### Test Booking (`test_booking.py`)

**Mục đích**: Test chức năng đặt lịch cho bệnh nhân

**Test cases**:
- ✅ `test_check_available_slots_success` - Kiểm tra slot thành công
- ✅ `test_check_available_slots_no_slots` - Không có slot
- ✅ `test_check_available_slots_with_shift_filter` - Lọc theo shift
- ✅ `test_recommend_medical_packages_success` - Đề xuất gói khám
- ✅ `test_recommend_medical_packages_no_match` - Không có gói phù hợp
- ✅ `test_create_booking_success` - Tạo booking thành công
- ✅ `test_create_booking_missing_fields` - Thiếu thông tin
- ✅ `test_create_booking_invalid_format` - Format không hợp lệ
- ✅ `test_create_booking_api_error` - API error
- ✅ `test_full_booking_flow_via_agent` - Full flow qua agent
- ✅ `test_booking_tools_not_initialized` - Tools chưa khởi tạo
- ✅ `test_booking_with_package_filter` - Lọc theo package

**Chạy**:
```bash
pytest tests/test_booking.py -v
```

### Test Memory (`test_memory.py`)

**Mục đích**: Test LangGraph memory hoạt động đúng

**Test cases**:
- ✅ `test_memory_initialization` - Khởi tạo memory
- ✅ `test_memory_persistence_same_session` - Memory persist trong session
- ✅ `test_memory_isolation_different_sessions` - Isolation giữa sessions
- ✅ `test_memory_clear_functionality` - Clear memory
- ✅ `test_conversation_history_retrieval` - Lấy conversation history
- ✅ `test_memory_with_booking_flow` - Memory trong booking flow
- ✅ `test_memory_with_clinic_info_queries` - Memory với clinic info
- ✅ `test_memory_session_id_generation` - Generate session ID
- ✅ `test_memory_with_agent_manager` - Memory qua AgentManager
- ✅ `test_memory_persistence_across_requests` - Persist qua requests
- ✅ `test_memory_error_handling` - Error handling

**Chạy**:
```bash
pytest tests/test_memory.py -v
```

### Integration Tests (`test_integration.py`)

**Mục đích**: Full integration test kết hợp với các service khác

**Test cases**:
- ✅ `test_integration_chat_endpoint_health` - Chat endpoint
- ✅ `test_integration_clinic_info_flow` - Clinic info flow
- ✅ `test_integration_booking_flow` - Booking flow
- ✅ `test_integration_multi_turn_conversation` - Multi-turn conversation
- ✅ `test_integration_doctor_search_flow` - Doctor search flow
- ✅ `test_integration_package_recommendation_flow` - Package recommendation
- ✅ `test_integration_error_recovery` - Error recovery
- ✅ `test_integration_session_management` - Session management
- ✅ `test_integration_chat_history_endpoint` - Chat history endpoint
- ✅ `test_integration_clear_session_endpoint` - Clear session endpoint
- ✅ `test_integration_full_user_journey` - Full user journey
- ✅ `test_integration_concurrent_sessions` - Concurrent sessions

**Chạy**:
```bash
pytest tests/test_integration.py -v
```

## 🔧 Configuration

### Pytest Configuration

File `pytest.ini` chứa cấu hình:
- Test discovery patterns
- Asyncio mode
- Output options
- Markers
- Logging

### Test Fixtures

File `conftest.py` chứa các fixtures:
- `mock_clinic_api` - Mock ClinicAPIService
- `mock_vector_store` - Mock PGVectorStore
- `initialized_agent` - LangGraphAgent đã khởi tạo
- `agent_manager` - AgentManager đã khởi tạo
- `sample_session_id` - Session ID mẫu
- `sample_patient_info` - Patient info mẫu

## 📊 Coverage

Để xem coverage:

```bash
pytest tests/ --cov=app --cov-report=html
open htmlcov/index.html
```

Hoặc:
```bash
./tests/run_all_tests.sh --coverage
```

## 🐛 Troubleshooting

### 1. Tests fail với "Tools chưa được khởi tạo"

**Nguyên nhân**: Tools chưa được init trước khi test

**Giải pháp**: Đảm bảo sử dụng fixtures `initialized_agent` hoặc `agent_manager`

### 2. Async tests không chạy

**Nguyên nhân**: `pytest-asyncio` chưa được cài hoặc cấu hình sai

**Giải pháp**:
```bash
pip install pytest-asyncio
```

Kiểm tra `pytest.ini` có `asyncio_mode = auto`

### 3. Memory tests không hoạt động

**Nguyên nhân**: Cần OpenAI API key hợp lệ

**Giải pháp**: Set `OPENAI_API_KEY` trong environment hoặc `.env`

### 4. Integration tests fail

**Nguyên nhân**: Services chưa chạy hoặc mocks không đúng

**Giải pháp**: 
- Kiểm tra mocks trong fixtures
- Đảm bảo services đang chạy nếu test thật

## 📌 Best Practices

1. **Luôn sử dụng fixtures** thay vì khởi tạo trực tiếp
2. **Mock external services** để tests chạy nhanh và độc lập
3. **Test từng chức năng riêng** trước khi test integration
4. **Kiểm tra error handling** không chỉ happy path
5. **Sử dụng descriptive test names** để dễ hiểu

## 🔗 Related Files

- `tests/conftest.py` - Test fixtures
- `tests/test_health.py` - Health check tests
- `tests/test_clinic_info.py` - Clinic info tests
- `tests/test_booking.py` - Booking tests
- `tests/test_memory.py` - Memory tests
- `tests/test_integration.py` - Integration tests
- `pytest.ini` - Pytest configuration
- `tests/run_all_tests.sh` - Bash test runner
- `tests/run_tests.py` - Python test runner

## 📞 Support

Nếu gặp vấn đề, kiểm tra:
1. Dependencies đã được cài đặt đầy đủ
2. Environment variables đã được set
3. Services đang chạy (nếu cần)
4. Logs trong `logs/ai_service.log`

