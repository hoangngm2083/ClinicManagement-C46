# API Gateway Setup Guide

## Quick Start

### 1. Build the API Gateway
```bash
# Windows
cd ApiGateway
mvnw.cmd clean package

# Linux/Mac
cd ApiGateway
./mvnw clean package
```

### 2. Run Standalone (Local Development)
```bash
# Make sure all microservices are running on their respective ports
java -jar target/ApiGateway-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### 3. Run with Docker Compose
```bash
# From project root
docker-compose up -d api-gateway
```

### 4. Build All Services Including Gateway
```bash
# From project root
mvn clean package -DskipTests
```

## Testing the Gateway

### 1. Health Check
```bash
curl http://localhost:8080/health
curl http://localhost:8080/actuator/health
```

### 2. View All Routes
```bash
curl http://localhost:8080/actuator/gateway/routes
```

### 3. Test Service Routing

**Auth Service:**
```bash
curl http://localhost:8080/api/auth/health
# Routes to: http://auth-service:8081/health
```

**Booking Service:**
```bash
curl http://localhost:8080/api/bookings/health
# Routes to: http://booking-service:8082/health
```

**Patient Service:**
```bash
curl http://localhost:8080/api/patients/health
# Routes to: http://patient-service:8088/health
```

**Staff Service:**
```bash
curl http://localhost:8080/api/staff/health
# Routes to: http://staff-service:8090/health
```

**Medical Package Service:**
```bash
curl http://localhost:8080/api/medical-packages/health
# Routes to: http://medical-package-service:8086/health
```

**Notification Service:**
```bash
curl http://localhost:8080/api/notifications/health
# Routes to: http://notification-service:8080/health
```

## Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────────┐
│   API Gateway       │  :8080
│  (Port 8080)        │
└──────┬──────────────┘
       │
       ├──────────────┐
       │              │
       ▼              ▼
┌─────────────┐  ┌─────────────┐
│ Auth        │  │ Booking     │
│ Service     │  │ Service     │
│ :8081       │  │ :8082       │
└─────────────┘  └─────────────┘
       │              │
       ▼              ▼
┌─────────────┐  ┌─────────────┐
│ Patient     │  │ Staff       │
│ Service     │  │ Service     │
│ :8088       │  │ :8090       │
└─────────────┘  └─────────────┘
       │              │
       ▼              ▼
┌─────────────┐  ┌─────────────┐
│MedPackage   │  │Notification │
│ Service     │  │ Service     │
│ :8086       │  │ :8080       │
└─────────────┘  └─────────────┘
```

## Configuration Profiles

### Local Profile (`application-local.properties`)
- Used for local development
- Services accessed via `localhost`
- Debug logging enabled

### Docker Profile (`application-docker.properties`)
- Used in Docker Compose
- Services accessed via Docker service names
- Production-level logging

### Default Profile (`application.properties`)
- Docker configuration by default
- Gateway configuration
- Circuit breaker settings
- CORS configuration

## Key Features

### 1. Request Routing
- Path-based routing with prefix stripping
- Example: `/api/auth/login` → `http://auth-service:8081/login`

### 2. Circuit Breaker
- Automatic circuit breaking on service failures
- Fallback responses when services are down
- Configurable thresholds and timeouts

### 3. CORS Support
- Configured for all origins (configurable)
- Supports all standard HTTP methods
- Pre-flight request handling

### 4. Logging
- Request/response logging
- Gateway route logging
- Service health logging

### 5. Health Checks
- Actuator health endpoint
- Individual service health monitoring
- Docker healthcheck integration

## Troubleshooting

### Gateway Not Starting
1. Check if port 8080 is available
2. Verify Java 17+ is installed
3. Check logs: `docker logs api-gateway`

### Service Routing Issues
1. Verify target service is running
2. Check service name resolution in Docker
3. Review gateway routes: `curl http://localhost:8080/actuator/gateway/routes`

### Connection Timeouts
1. Increase timeout in `application.properties`:
   ```properties
   spring.cloud.gateway.httpclient.response-timeout=10s
   ```
2. Check network connectivity between containers

### CORS Errors
1. Verify CORS configuration in `CorsConfig.java`
2. Check browser console for specific CORS error
3. Ensure preflight OPTIONS requests are handled

## Advanced Configuration

### Adding a New Route
1. Add route in `application.properties`:
   ```properties
   spring.cloud.gateway.routes[N].id=new-service
   spring.cloud.gateway.routes[N].uri=http://new-service:PORT
   spring.cloud.gateway.routes[N].predicates[0]=Path=/api/new/**
   spring.cloud.gateway.routes[N].filters[0]=StripPrefix=1
   ```

2. Or add in `GatewayConfig.java`:
   ```java
   .route("new-service", r -> r
       .path("/api/new/**")
       .filters(f -> f.stripPrefix(1))
       .uri("lb://new-service"))
   ```

### Rate Limiting (Optional)
Enable Redis and add rate limiting filter:
```properties
spring.redis.host=localhost
spring.redis.port=6379
```

### Authentication Filter (Future)
Add JWT validation in a global filter for protected routes.

## Production Considerations

1. **Security**
   - Add authentication/authorization filters
   - Implement JWT validation
   - Configure proper CORS origins

2. **Performance**
   - Increase connection pool size
   - Configure appropriate timeouts
   - Enable response caching where appropriate

3. **Monitoring**
   - Integrate with Prometheus/Grafana
   - Set up alerting for circuit breaker trips
   - Monitor gateway metrics

4. **Scalability**
   - Run multiple gateway instances
   - Use load balancer in front of gateway
   - Consider service mesh for large deployments
