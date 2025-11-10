# API Gateway - Quick Reference Card

## 🚀 Quick Commands

### Build
```bash
# Build only API Gateway
cd ApiGateway
./mvnw clean package

# Build all services (from root)
mvn clean package -DskipTests
```

### Run
```bash
# Run locally
java -jar ApiGateway/target/ApiGateway-0.0.1-SNAPSHOT.jar --spring.profiles.active=local

# Run with Docker
docker-compose up -d api-gateway

# Run all services
docker-compose up -d
```

### Test
```bash
# Health check
curl http://localhost:8080/health

# View routes
curl http://localhost:8080/actuator/gateway/routes

# Test auth service routing
curl http://localhost:8080/api/auth/health
```

## 📍 Service Endpoints (via Gateway)

| Service | Direct Port | Gateway URL | Example |
|---------|------------|-------------|---------|
| Auth | 8081 | `/api/auth/**` | `http://localhost:8080/api/auth/login` |
| Booking | 8082 | `/api/bookings/**` | `http://localhost:8080/api/bookings/create` |
| Notification | 8080 | `/api/notifications/**` | `http://localhost:8080/api/notifications/send` |
| Medical Package | 8086 | `/api/medical-packages/**` | `http://localhost:8080/api/medical-packages/list` |
| Patient | 8088 | `/api/patients/**` | `http://localhost:8080/api/patients/register` |
| Staff | 8090 | `/api/staff/**` | `http://localhost:8080/api/staff/list` |

## 🔍 Monitoring Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /health` | Simple health check |
| `GET /actuator/health` | Detailed health info |
| `GET /actuator/gateway/routes` | All configured routes |
| `GET /actuator/info` | Gateway information |

## 🐳 Docker Commands

```bash
# Build image
docker-compose build api-gateway

# Start gateway
docker-compose up -d api-gateway

# View logs
docker logs api-gateway -f

# Restart gateway
docker-compose restart api-gateway

# Stop gateway
docker-compose stop api-gateway

# Remove gateway
docker-compose down api-gateway
```

## ⚙️ Configuration Profiles

| Profile | Use Case | Service URLs |
|---------|----------|-------------|
| `local` | Local development | `localhost:PORT` |
| `docker` | Docker Compose | Service names (e.g., `auth-service:8081`) |

## 🛠️ Troubleshooting

| Issue | Solution |
|-------|----------|
| Port 8080 in use | Change port in `application.properties`: `server.port=8081` |
| Service not found | Check service is running: `docker ps` |
| CORS error | Verify CORS config in `CorsConfig.java` |
| Timeout | Increase timeout: `spring.cloud.gateway.httpclient.response-timeout=10s` |
| Circuit breaker open | Check service health, wait for reset (10s) |

## 📋 File Locations

| File | Purpose |
|------|---------|
| `application.properties` | Main config (Docker) |
| `application-local.properties` | Local dev config |
| `application-docker.properties` | Docker-specific config |
| `CorsConfig.java` | CORS settings |
| `GatewayConfig.java` | Route definitions |
| `LoggingFilter.java` | Request logging |
| `FallbackController.java` | Error handling |

## 🎯 Key Features

- ✅ Centralized routing
- ✅ CORS support
- ✅ Circuit breaker (Resilience4j)
- ✅ Request/response logging
- ✅ Health checks
- ✅ Fallback handlers
- ✅ Docker ready
- ✅ Multi-profile support

## 📞 Support

For detailed setup instructions, see:
- `README.md` - Overview
- `SETUP_GUIDE.md` - Complete guide
- `PROJECT_SUMMARY.md` - Technical details

---

**Gateway Port:** 8080  
**Health Check:** http://localhost:8080/health  
**Routes Info:** http://localhost:8080/actuator/gateway/routes
