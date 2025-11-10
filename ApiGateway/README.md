# API Gateway for Clinic Management System

## Overview
This API Gateway serves as the single entry point for all microservices in the Clinic Management System. It provides routing, load balancing, circuit breaking, and cross-cutting concerns.

## Features
- **Centralized Routing**: Routes requests to appropriate microservices
- **CORS Support**: Configured for cross-origin requests
- **Circuit Breaker**: Resilience4j integration for fault tolerance
- **Health Checks**: Actuator endpoints for monitoring
- **Request Logging**: Global filter for request/response logging
- **Fallback Handlers**: Graceful degradation when services are unavailable

## Service Routes

| Path Prefix | Target Service | Port |
|------------|---------------|------|
| `/api/auth/**` | Auth Service | 8081 |
| `/api/bookings/**` | Booking Service | 8082 |
| `/api/notifications/**` | Notification Service | 8080 |
| `/api/medical-packages/**` | Medical Package Service | 8086 |
| `/api/patients/**` | Patient Service | 8088 |
| `/api/staff/**` | Staff Service | 8090 |

## Running Locally

### Prerequisites
- Java 17+
- Maven 3.6+

### Build
```bash
mvn clean package
```

### Run
```bash
# Use local profile to connect to localhost services
java -jar target/ApiGateway-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### Run with Docker
```bash
docker-compose up -d api-gateway
```

## Endpoints

### Health Check
```
GET http://localhost:8080/health
GET http://localhost:8080/actuator/health
```

### Gateway Routes
```
GET http://localhost:8080/actuator/gateway/routes
```

## Configuration

### Environment Variables
- `SPRING_PROFILES_ACTIVE`: Profile to activate (docker, local, prod)
- `JAVA_OPTS`: JVM options (default: -Xmx512m -Xms256m)
- `TZ`: Timezone (default: Asia/Ho_Chi_Minh)

### Circuit Breaker Settings
- Sliding window size: 10
- Minimum calls: 5
- Failure rate threshold: 50%
- Wait duration in open state: 10s

## Testing

### Test Auth Service via Gateway
```bash
curl http://localhost:8080/api/auth/health
```

### Test Booking Service via Gateway
```bash
curl http://localhost:8080/api/bookings/health
```

## Monitoring
Access actuator endpoints:
- Health: `http://localhost:8080/actuator/health`
- Gateway routes: `http://localhost:8080/actuator/gateway/routes`
- Info: `http://localhost:8080/actuator/info`

## Architecture Notes
- Uses Spring Cloud Gateway (reactive/non-blocking)
- WebFlux-based (Netty server)
- Stateless design for horizontal scaling
- Load balancing ready (use `lb://` scheme with service discovery)
