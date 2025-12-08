# AI Service Route Configuration

## 📋 Tổng Quan

AI Service đã được thêm vào API Gateway với route `/api/ai/**`. Tất cả endpoints trong AI Service đều sử dụng prefix `/ai` và có thể được cấu hình qua biến môi trường `AI_API_PREFIX`.

## 🔧 Cấu Hình

### Route Details

- **Route ID**: `ai-service`
- **Path Pattern**: `/api/ai/**`
- **Local URI**: `http://localhost:8000`
- **Docker URI**: `http://ai-service:8000`
- **Filter**: `StripPrefix=1` (strips `/api/ai` prefix)

### Files Updated

1. `application.properties` - Default configuration (local)
2. `application-docker.properties` - Docker configuration
3. `application-local.properties` - Local development configuration
4. `AIService/app/config/settings.py` - Added `ai_api_prefix` setting
5. `AIService/app/main.py` - Updated all endpoints to use `/ai` prefix

## 🌐 Endpoints Available Through Gateway

### Chat Endpoint
```
POST /api/ai/chat
Content-Type: application/json

Body:
{
  "message": "Xin chào",
  "session_id": "optional-session-id"
}

Response:
{
  "response": "AI response text",
  "suggested_actions": ["action1", "action2"],
  "session_id": "session-id",
  "timestamp": "2025-12-06T...",
  "error": null
}
```

### Health Check
```
GET /api/ai/health

Response:
{
  "status": "healthy|unhealthy",
  "version": "1.0.0",
  "services": {
    "vector_store": true|false,
    "clinic_api": true|false,
    "agent": true|false
  }
}
```

### Service Info
```
GET /api/ai/info

Response:
{
  "name": "Clinic AI Service",
  "version": "1.0.0",
  "description": "AI-powered chatbot for clinic management",
  "capabilities": [...],
  "supported_languages": ["vi"],
  "rate_limit": "100 requests per 60 seconds"
}
```

### Chat History
```
GET /api/ai/chat/history/{session_id}

Response:
{
  "session_id": "session-id",
  "history": [...]
}
```

### Clear Session
```
DELETE /api/ai/chat/session/{session_id}

Response:
{
  "message": "Session {session_id} cleared successfully"
}
```

## 🧪 Testing

### Test via Gateway (Docker)
```bash
# Health check
curl http://localhost:8080/api/ai/health

# Chat
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Xin chào", "session_id": "test123"}'

# Info
curl http://localhost:8080/api/ai/info
```

### Test Direct (Bypass Gateway)
```bash
# Direct access to AI Service
curl http://localhost:8000/health
curl http://localhost:8000/info
```

## ⚙️ Configuration Details

### Route Configuration
```properties
# Routes for AI Service
spring.cloud.gateway.routes[7].id=ai-service
spring.cloud.gateway.routes[7].uri=http://ai-service:8000  # Docker
# spring.cloud.gateway.routes[7].uri=http://localhost:8000  # Local
spring.cloud.gateway.routes[7].predicates[0]=Path=/api/ai/**
spring.cloud.gateway.routes[7].filters[0]=StripPrefix=1
```

### Path Mapping

| Gateway Path | After StripPrefix | AI Service Endpoint |
|--------------|-------------------|---------------------|
| `/api/ai/chat` | `/ai/chat` | `POST /ai/chat` |
| `/api/ai/health` | `/ai/health` | `GET /ai/health` |
| `/api/ai/info` | `/ai/info` | `GET /ai/info` |
| `/api/ai/chat/history/{id}` | `/ai/chat/history/{id}` | `GET /ai/chat/history/{id}` |
| `/api/ai/chat/session/{id}` | `/ai/chat/session/{id}` | `DELETE /ai/chat/session/{id}` |

## 🔄 Restart Required

Sau khi thêm route, cần restart API Gateway:

```bash
# Docker
docker-compose restart api-gateway

# Local
# Restart Spring Boot application
```

## ✅ Verification

Sau khi restart, verify route:

```bash
# Check gateway routes
curl http://localhost:8080/actuator/gateway/routes | grep ai-service

# Test health endpoint
curl http://localhost:8080/api/ai/health
```

## 📝 Notes

1. **Timeout**: AI Service có thể mất thời gian xử lý lâu hơn. Nếu cần, có thể tăng timeout trong gateway config:
   ```properties
   spring.cloud.gateway.httpclient.response-timeout=30s
   ```

2. **CORS**: CORS đã được cấu hình global trong gateway, nên AI Service endpoints sẽ tự động có CORS support.

3. **Circuit Breaker**: Có thể thêm circuit breaker cho AI Service route nếu cần:
   ```properties
   spring.cloud.gateway.routes[7].filters[1]=CircuitBreaker=ai-service-circuit-breaker
   ```

4. **Rate Limiting**: AI Service có rate limiting riêng (100 requests/60s), gateway có thể thêm rate limiting nếu cần.

## 🔗 Related Files

- `ApiGateway/src/main/resources/application.properties`
- `ApiGateway/src/main/resources/application-docker.properties`
- `ApiGateway/src/main/resources/application-local.properties`
- `AIService/app/main.py` - AI Service endpoints

