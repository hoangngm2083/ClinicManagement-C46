# AI Service Tests

Bộ test đầy đủ cho AI Service, bao gồm các chức năng chính và integration tests.

## 📋 Cấu trúc Tests

### 1. `test_health.py`
Tests cho các health check endpoints và basic functionality.

### 2. `test_clinic_info.py`
Tests cho chức năng **cung cấp thông tin phòng khám**:
- Tìm kiếm thông tin từ vector store
- Xử lý queries về giờ hoạt động, quy trình, FAQ
- Error handling

### 3. `test_booking.py`
Tests cho chức năng **đặt lịch cho bệnh nhân**:
- Kiểm tra slot trống
- Đề xuất gói khám
- Tạo booking
- Validation và error handling

### 4. `test_memory.py`
Tests cho **LangGraph memory functionality**:
- Memory persistence trong cùng session
- Memory isolation giữa các sessions
- Conversation history
- Multi-turn conversations

### 5. `test_integration.py`
**Full integration tests** kết hợp với các service khác:
- End-to-end user journeys
- Multi-service interactions
- Concurrent sessions
- Error recovery

## 🚀 Chạy Tests

### Chạy tất cả tests
```bash
cd AIService
pytest tests/
```

### Chạy với script
```bash
cd AIService
./tests/run_all_tests.sh
```

### Chạy với coverage
```bash
pytest tests/ --cov=app --cov-report=html
./tests/run_all_tests.sh --coverage
```

### Chạy từng nhóm tests
```bash
# Chỉ test clinic info
pytest tests/test_clinic_info.py -v

# Chỉ test booking
pytest tests/test_booking.py -v

# Chỉ test memory
pytest tests/test_memory.py -v

# Chỉ test integration
pytest tests/test_integration.py -v
```

### Chạy test cụ thể
```bash
pytest tests/test_clinic_info.py::test_get_clinic_info_success -v
```

## 🧪 Test Fixtures

Tests sử dụng các fixtures trong `conftest.py`:

- `mock_clinic_api`: Mock ClinicAPIService với dữ liệu mẫu
- `mock_vector_store`: Mock PGVectorStore với search results
- `initialized_agent`: LangGraphAgent đã được khởi tạo
- `agent_manager`: AgentManager đã được khởi tạo
- `sample_session_id`: Session ID mẫu
- `sample_patient_info`: Thông tin bệnh nhân mẫu

## 📝 Test Cases

### Clinic Information Tests
- ✅ Tìm kiếm thông tin thành công
- ✅ Xử lý khi không có kết quả
- ✅ Tìm kiếm trong clinic processes
- ✅ Tìm kiếm trong FAQ
- ✅ Error handling
- ✅ Qua agent

### Booking Tests
- ✅ Kiểm tra slot trống
- ✅ Lọc theo shift
- ✅ Đề xuất gói khám
- ✅ Tạo booking thành công
- ✅ Validation thông tin
- ✅ Error handling
- ✅ Full booking flow

### Memory Tests
- ✅ Memory initialization
- ✅ Memory persistence
- ✅ Session isolation
- ✅ Conversation history
- ✅ Multi-turn conversations
- ✅ Memory clearing
- ✅ Session ID generation

### Integration Tests
- ✅ Chat endpoint
- ✅ Clinic info flow
- ✅ Booking flow
- ✅ Multi-turn conversation
- ✅ Doctor search
- ✅ Package recommendation
- ✅ Error recovery
- ✅ Session management
- ✅ Full user journey
- ✅ Concurrent sessions

## 🔧 Requirements

Các dependencies cần thiết:
```bash
pytest==7.4.3
pytest-asyncio==0.21.1
pytest-cov==4.1.0  # Optional, for coverage
```

## 📊 Coverage

Để xem coverage report:
```bash
pytest tests/ --cov=app --cov-report=html
open htmlcov/index.html
```

## 🐛 Troubleshooting

### Tests fail với "Tools chưa được khởi tạo"
- Đảm bảo fixtures được sử dụng đúng cách
- Kiểm tra `init_tools()` được gọi trong fixtures

### Async tests không chạy
- Đảm bảo `pytest-asyncio` đã được cài đặt
- Kiểm tra `asyncio_mode = auto` trong `pytest.ini`

### Memory tests không hoạt động
- LangGraph memory cần OpenAI API key hợp lệ
- Kiểm tra `OPENAI_API_KEY` trong environment

## 📌 Notes

- Tests sử dụng mocks để không cần services thật chạy
- Integration tests có thể cần services đang chạy
- Một số tests phụ thuộc vào LLM responses (có thể không ổn định)

