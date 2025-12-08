# Clinic AI Service

AI-powered chatbot service for clinic management and appointment booking using LangChain, OpenAI GPT-4, and ChromaDB.

## 🚀 Features

- **Doctor Information Search**: Find doctors by specialty, name, or department
- **Appointment Booking**: Intelligent booking system with slot availability checking
- **Medical Package Recommendations**: AI-powered package suggestions based on symptoms
- **Clinic Information Queries**: Answer questions about clinic policies, procedures, and services
- **RAG (Retrieval-Augmented Generation)**: Accurate information from vector database
- **Real-time Data Sync**: Automatic synchronization with microservices

## 🏗️ Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Chatbot UI    │────│  AI Service      │────│ Microservices   │
│  (Web/Mobile)   │    │  (FastAPI)       │    │  (Spring Boot)  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌──────────────────┐
                       │   ChromaDB       │
                       │  (Vector Store)  │
                       └──────────────────┘
```

## 🛠️ Technology Stack

- **AI Framework**: LangChain
- **LLM**: OpenAI GPT-4o
- **Embeddings**: OpenAI text-embedding-ada-002
- **Vector Database**: ChromaDB
- **API Framework**: FastAPI
- **Language**: Python 3.11+
- **Container**: Docker

## 📋 Prerequisites

- Python 3.11+
- Docker & Docker Compose
- OpenAI API Key
- Access to Clinic Microservices

## 🚀 Quick Start

### 1. Clone and Setup

```bash
cd AIService
cp env.example .env
# Edit .env with your configuration
```

### 2. Configure Environment

Edit `.env` file with your settings:

```env
OPENAI_API_KEY=your_openai_api_key_here
CLINIC_API_BASE_URL=http://api-gateway:8080
AI_SERVICE_PORT=8000
```

### 3. Run with Docker

```bash
# Build and run
docker build -t clinic-ai-service .
docker run -p 8000:8000 --env-file .env clinic-ai-service
```

### 4. Run with Docker Compose

The AI service is integrated into the main docker-compose.yml:

```bash
docker-compose up ai-service
```

## 📚 API Documentation

### Main Endpoints

#### POST `/chat`
Main chat endpoint for AI interaction.

**Request:**
```json
{
  "message": "Tôi muốn đặt lịch khám răng",
  "session_id": "user123"
}
```

**Response:**
```json
{
  "response": "Tôi sẽ giúp bạn đặt lịch khám răng...",
  "suggested_actions": ["book_appointment", "view_doctors"],
  "session_id": "user123",
  "timestamp": "2024-01-01T10:00:00"
}
```

#### GET `/health`
Health check endpoint.

#### Admin Endpoints

**POST `/admin/clear-prompt-cache`**
Clear system prompt cache to force refresh from database.

**GET `/admin/prompt-preview`**
Preview current system prompt content (for debugging).

#### GET `/chat/history/{session_id}`
Get conversation history for a session.

## 🤖 AI Agent Capabilities

### Dynamic System Prompt
AI Agent sử dụng **Dynamic System Prompt** - tự động load thông tin gói khám mới nhất từ database thay vì hardcode. Điều này đảm bảo:

- ✅ Luôn hiển thị gói khám hiện tại
- ✅ Tự động cập nhật khi có gói mới
- ✅ Cache 1 giờ để tối ưu performance
- ✅ Fallback an toàn khi database lỗi

### Tools Available

1. **search_doctor_info**: Find doctors by name, specialty, or department
2. **check_available_slots**: Check appointment slots availability
3. **recommend_medical_packages**: Suggest packages based on symptoms
4. **create_booking**: Create new appointment bookings
5. **get_clinic_info**: Answer general clinic information queries
6. **get_doctor_schedule**: Get doctor working schedules

### Conversation Flow

```
User Input → Agent Reasoning → Tool Selection → API Calls → Response Generation
```

### Example Conversations

#### Booking Appointment
```
User: "Tôi bị đau răng, muốn khám"
Agent: "Tôi hiểu bạn đang gặp vấn đề về răng. Phòng khám có bác sĩ răng miệng chuyên nghiệp. Bạn có thể cho tôi biết triệu chứng cụ thể không?"

User: "Răng số 6 bị đau, có lỗ sâu"
Agent: "Dựa trên triệu chứng, tôi đề xuất gói khám răng cơ bản. Bạn muốn đặt lịch vào ngày nào?"

User: "Ngày mai được không?"
Agent: "Tôi sẽ kiểm tra slot trống ngày mai... Có slot vào buổi sáng. Bạn có thể cung cấp thông tin cá nhân để đặt lịch được không?"
```

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `OPENAI_API_KEY` | OpenAI API Key | Required |
| `CLINIC_API_BASE_URL` | Base URL of clinic API gateway | `http://api-gateway:8080` |
| `AI_SERVICE_PORT` | Port for AI service | `8000` |
| `CHROMA_DB_PATH` | Path to ChromaDB storage | `./chroma_db` |
| `MEMORY_MAX_TOKENS` | Max tokens for conversation memory | `2000` |

### Data Synchronization

The service automatically syncs data from microservices:

- **Doctors**: Every 15 minutes
- **Medical Packages**: Every 30 minutes
- **Slot Availability**: Every 5 minutes

## 🧪 Testing

### Run Tests
```bash
pytest tests/
```

### Manual Testing
```bash
# Health check
curl http://localhost:8000/health

# Chat test
curl -X POST http://localhost:8000/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Xin chào", "session_id": "test"}'
```

## 📊 Monitoring

### Health Endpoints
- `/health` - Overall service health
- `/metrics` - Prometheus metrics (if configured)

### Logs
- Console logs for real-time monitoring
- File logs in `logs/ai_service.log`

## 🔒 Security

- Input sanitization and validation
- Rate limiting (100 requests per minute)
- HTTPS recommended for production
- API key authentication for external access

## 🚀 Deployment

### Production Considerations

1. **Environment Variables**: Use Docker secrets or Kubernetes secrets
2. **Scaling**: Run multiple instances behind a load balancer
3. **Database**: Use external ChromaDB or Pinecone for production
4. **Monitoring**: Integrate with ELK stack or similar
5. **Backup**: Regular vector database backups

### Docker Compose Production
```yaml
ai-service:
  build: .
  environment:
    - OPENAI_API_KEY=${OPENAI_API_KEY}
    - CLINIC_API_BASE_URL=${CLINIC_API_BASE_URL}
  volumes:
    - chroma_data:/app/chroma_db
  depends_on:
    - api-gateway
  networks:
    - clinic-net
```

## 🐛 Troubleshooting

### Common Issues

1. **OpenAI API Errors**
   - Check API key validity
   - Verify API quota and billing

2. **Clinic API Connection**
   - Ensure microservices are running
   - Check network connectivity
   - Verify API gateway routing

3. **Vector DB Issues**
   - Check disk space
   - Verify ChromaDB persistence
   - Clear and rebuild vector store if corrupted

### Debug Mode
```bash
# Enable debug logging
export PYTHONPATH=/app
python -m uvicorn app.main:app --reload --log-level debug
```

## 📝 Development

### Project Structure
```
AIService/
├── app/
│   ├── agents/          # AI agent core logic
│   ├── rag/            # RAG pipeline and vector store
│   ├── services/       # Clinic API integration
│   ├── models/         # Pydantic models and prompts
│   ├── utils/          # Helper functions
│   └── config/         # Configuration management
├── tests/              # Unit and integration tests
├── requirements.txt    # Python dependencies
├── Dockerfile         # Docker configuration
└── README.md          # This file
```

### Adding New Tools

1. Create tool function in `app/agents/tools.py`
2. Add `@tool` decorator
3. Update agent initialization
4. Add to system prompt if needed

### Extending RAG

1. Add new data sources in `app/rag/data_loader.py`
2. Create new collection in vector store
3. Update retrieval logic in tools

## 🤝 Contributing

1. Fork the repository
2. Create feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit pull request

## 📄 License

This project is part of the Clinic Management System.

## 📞 Support

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the troubleshooting section above
