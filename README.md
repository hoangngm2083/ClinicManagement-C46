# Clinic Management System - Microservices Architecture

A comprehensive clinic management system built with **Spring Boot microservices architecture**, implementing **CQRS/Event Sourcing** patterns with **Axon Framework**, and featuring an **AI-powered chatbot** for patient assistance.

## 🎯 Project Overview

This project demonstrates a production-ready microservices application for managing clinic operations, including patient registration, appointment booking, medical package management, examination workflows, payment processing, and AI-assisted patient support.

**Target Position**: Java Developer  
**Tech Stack**: Java 17, Spring Boot 3.x, Axon Framework, PostgreSQL, Docker, Python (AI Service)

---

## 🏗️ System Architecture

### Microservices Overview

The system consists of **12 independent microservices**, each with its own database following the **Database-per-Service** pattern:

| Service | Port | Responsibility | Database |
|---------|------|----------------|----------|
| **API Gateway** | 8080 | Single entry point, routing, load balancing | - |
| **Auth Service** | 8081 | Authentication, authorization, JWT token management | auth_db |
| **Booking Service** | 8082 | Appointment booking with Saga orchestration | booking_db |
| **Notification Service** | 8083 | Email notifications, event-driven messaging | notification_db |
| **File Service** | 8084 | File upload/download with AWS S3 integration | file_db |
| **Medical Package Service** | 8086 | Medical packages, services, and pricing | medical_package_db |
| **Patient Service** | 8088 | Patient profile management | patient_db |
| **Staff Service** | 8090 | Staff, doctor, and department management | staff_db |
| **Examination Flow Service** | 9093 | Queue management with Redis, examination workflow | examination_flow_db |
| **Examination Service** | 9094 | Medical examination records | examination_db |
| **Payment Service** | 9098 | Payment processing with VNPay integration | payment_db |
| **AI Service** | 8000 | AI chatbot with LangChain, RAG, and LangGraph | vector_db |

### Infrastructure Components

- **Axon Server** (8024): Event Store and Message Routing for CQRS/ES
- **PostgreSQL** (5432): Primary database with pgvector extension for AI embeddings
- **Redis** (6379): Caching and queue management
- **Nginx**: Reverse proxy and SSL termination (production)

---

## 💡 Key Technical Highlights

### 1. **CQRS & Event Sourcing with Axon Framework**

Implemented **Command Query Responsibility Segregation** and **Event Sourcing** patterns using Axon Framework 4.11.2:

- **Command Side**: Aggregates handle business logic and emit domain events
- **Query Side**: Projections maintain read-optimized views
- **Event Store**: Axon Server stores all domain events for complete audit trail
- **Saga Pattern**: Orchestrates distributed transactions (e.g., `BookingProcessingSaga`)

**Example Implementation** (Booking Service):
```java
// Command Handler in Aggregate
@CommandHandler
public void handle(CreateBookingCommand command) {
    // Business validation
    AggregateLifecycle.apply(new BookingCreatedEvent(...));
}

// Event Handler in Projection
@EventHandler
public void on(BookingCreatedEvent event) {
    // Update read model
    bookingViewRepository.save(new BookingView(event));
}
```

### 2. **Distributed Transaction Management**

Implemented **Saga Pattern** to handle complex business workflows across multiple services:

- **BookingProcessingSaga**: Coordinates booking creation, payment, and notification
- Automatic compensation on failure (rollback mechanisms)
- Event-driven communication between services

### 3. **AI-Powered Patient Assistant**

Built an intelligent chatbot using **Python FastAPI** with advanced AI technologies:

- **LangChain**: Framework for LLM application development
- **LangGraph**: State machine for conversation flow management
- **RAG (Retrieval Augmented Generation)**: 
  - Vector embeddings stored in PostgreSQL with pgvector
  - Semantic search for doctor information, medical packages, and FAQs
  - OpenAI embeddings (1536 dimensions)
- **Tool Calling**: Integration with clinic APIs for real-time data
- **Medical Symptom Analyzer**: Rule-based system for accurate medical package recommendations

**Key Features**:
- Appointment booking assistance
- Doctor and medical package recommendations
- Clinic information Q&A
- Symptom-based consultation

### 4. **API Gateway Pattern**

Centralized routing and cross-cutting concerns:
- Request routing to appropriate microservices
- Authentication/Authorization enforcement
- Rate limiting and load balancing
- Centralized logging and monitoring

### 5. **Event-Driven Architecture**

All services communicate asynchronously through domain events:
- Loose coupling between services
- High scalability and fault tolerance
- Event replay capability for debugging and recovery

### 6. **Third-Party Integrations**

- **AWS S3**: File storage with CloudFront CDN
- **VNPay**: Payment gateway integration
- **Gmail SMTP**: Email notification service
- **OpenAI API**: GPT models for AI chatbot

---

## 🛠️ Technologies & Frameworks

### Backend (Java)
- **Spring Boot 3.5.7**: Core framework
- **Spring Data JPA**: Database access and ORM
- **Spring Web**: RESTful API development
- **Spring Actuator**: Health checks and monitoring
- **Axon Framework 4.11.2**: CQRS/Event Sourcing
- **Lombok**: Boilerplate code reduction
- **SpringDoc OpenAPI**: API documentation (Swagger)
- **Spring Retry**: Automatic retry mechanisms

### AI Service (Python)
- **FastAPI**: High-performance web framework
- **LangChain**: LLM application framework
- **LangGraph**: Conversation state management
- **OpenAI API**: GPT-4 and embeddings
- **pgvector**: Vector similarity search
- **SQLAlchemy**: Database ORM

### Database & Storage
- **PostgreSQL 15**: Primary database with pgvector extension
- **Redis**: Caching and queue management
- **AWS S3**: Object storage
- **Axon Server**: Event store

### DevOps & Deployment
- **Docker & Docker Compose**: Containerization
- **Maven**: Build automation
- **Nginx**: Reverse proxy and SSL

---

## 📋 Core Features

### Patient Management
- Patient registration and profile management
- Medical history tracking
- Appointment booking with real-time slot availability

### Appointment System
- Multi-shift scheduling (Morning, Afternoon, Evening)
- Doctor availability management
- Automated booking confirmation via email
- Queue management with Redis

### Medical Package Management
- CRUD operations for medical packages and services
- Bulk import/export via CSV
- Pricing management
- Soft delete implementation

### Examination Workflow
- Queue-based patient flow
- Real-time status updates
- Examination record management
- Integration with payment service

### Payment Processing
- VNPay payment gateway integration
- Payment status tracking
- Invoice generation
- Automated receipt emails

### AI Chatbot
- Natural language conversation
- Appointment booking assistance
- Medical package recommendations based on symptoms
- Doctor search and information retrieval

---

## 🧪 Testing

The project includes comprehensive testing coverage:

- **Unit Tests**: Service layer and business logic validation
- **Integration Tests**: API endpoint testing
- **Event Handler Tests**: Event sourcing workflow verification
- **Saga Tests**: Distributed transaction testing

---

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Docker & Docker Compose
- Maven 3.8+
- Node.js 18+ (for frontend, if applicable)

### Running the Application

1. **Clone the repository**
```bash
git clone <repository-url>
cd BE
```

2. **Configure environment variables**
```bash
cp .env.example .env
# Edit .env with your configurations (AWS, OpenAI API keys, etc.)
```

3. **Start all services with Docker Compose**
```bash
docker-compose up -d
```

4. **Access the services**
- API Gateway: http://localhost:8080
- Axon Server Dashboard: http://localhost:8024
- AI Service: http://localhost:8000
- Individual services: Check docker-compose.yml for port mappings

### Building Individual Services

```bash
# Build all services
mvn clean install

# Build specific service
cd BookingService
mvn clean package
```

### Production Deployment to AWS EC2

The project includes production-ready deployment configurations for AWS EC2 with SSL/TLS encryption:

**Deployment Files**:
- `docker-compose.deploy.yml`: Production-optimized Docker Compose configuration
- `ec2.push.sh`: Automated deployment script for pushing code to EC2
- `setup-ssl.sh`: SSL certificate management with Let's Encrypt
- `nginx/nginx.conf`: Nginx reverse proxy with SSL termination

**Key Production Features**:
- ✅ **SSL/TLS Encryption**: Let's Encrypt certificates with auto-renewal
- ✅ **Nginx Reverse Proxy**: Load balancing and SSL termination
- ✅ **Security Headers**: HSTS, CSP, X-Frame-Options
- ✅ **WebSocket Support**: Secure WSS connections for real-time features
- ✅ **Health Checks**: Automated service health monitoring
- ✅ **Resource Optimization**: Production-tuned JVM settings and database configurations

**Deployment Process**:
```bash
# 1. Push code to EC2
./ec2.push.sh --file=.env.prod --file=docker-compose.deploy.yml

# 2. SSH to EC2 and setup SSL (first time only)
ssh -i ~/.ssh/your-key.pem ubuntu@your-ec2-ip
./setup-ssl.sh obtain

# 3. Deploy services
docker-compose -f docker-compose.deploy.yml up -d --build

# 4. Verify deployment
curl https://your-domain.com/actuator/health
```

**Production Environment**:
- **Domain**: clinic46.duckdns.org (example)
- **SSL Certificate**: Let's Encrypt (auto-renewal via cron)
- **Database**: PostgreSQL with production-optimized configurations
- **Monitoring**: Spring Boot Actuator health endpoints
- **Logging**: Centralized logging with volume mounts

---

## 📊 Database Schema

Each microservice maintains its own database schema:

- **Command Side**: Aggregate state and domain events (Axon tables)
- **Query Side**: Denormalized views optimized for reads
- **Soft Delete**: Implemented using `deleted` flag and `deletedAt` timestamp
- **Audit Fields**: `createdAt`, `updatedAt` for all entities

---

## 🔐 Security

- **JWT Authentication**: Token-based authentication via Auth Service
- **Role-Based Access Control (RBAC)**: Different permissions for patients, staff, and doctors
- **API Gateway Security**: Centralized authentication enforcement
- **Environment Variables**: Sensitive data stored in .env files
- **HTTPS**: SSL/TLS encryption in production (Nginx with Let's Encrypt)

---

## 📈 Scalability & Performance

- **Horizontal Scaling**: Each microservice can be scaled independently
- **Database Connection Pooling**: Optimized PostgreSQL configurations
- **Redis Caching**: Reduced database load for frequently accessed data
- **Event-Driven**: Asynchronous processing for non-blocking operations
- **CDN Integration**: CloudFront for static file delivery

---

## 🎓 Learning Outcomes & Skills Demonstrated

As a **Java Developer**, this project showcases my proficiency in:

### Core Java & Spring Boot
✅ RESTful API design and implementation  
✅ Spring Data JPA and database modeling  
✅ Dependency Injection and IoC container  
✅ Exception handling and validation  
✅ Logging and monitoring with Actuator  

### Advanced Architecture Patterns
✅ Microservices architecture design  
✅ CQRS and Event Sourcing with Axon Framework  
✅ Saga pattern for distributed transactions  
✅ Event-driven architecture  
✅ Domain-Driven Design (DDD) principles  

### Database & Persistence
✅ PostgreSQL database design and optimization  
✅ JPA/Hibernate ORM mapping  
✅ Database migration and versioning  
✅ Query optimization and indexing  

### DevOps & Deployment
✅ Docker containerization  
✅ Docker Compose orchestration  
✅ CI/CD pipeline setup  
✅ Environment configuration management  

### Integration & Communication
✅ Third-party API integration (AWS S3, VNPay, OpenAI)  
✅ Email service integration  
✅ Inter-service communication patterns  
✅ API Gateway implementation  

### Software Engineering Best Practices
✅ Clean Code principles  
✅ SOLID principles  
✅ Git version control  
✅ API documentation with OpenAPI/Swagger  
✅ Error handling and retry mechanisms  

---

## 📝 API Documentation

API documentation is available via Swagger UI for each service:

- Booking Service: http://localhost:8082/swagger-ui.html
- Medical Package Service: http://localhost:8086/swagger-ui.html
- Staff Service: http://localhost:8090/swagger-ui.html
- *Similar endpoints for other services*

---

## 🔄 Future Enhancements

- [ ] Complete unit and integration test coverage
- [ ] Implement distributed tracing with Zipkin/Jaeger
- [ ] Add Kubernetes deployment configurations
- [ ] Implement GraphQL API layer
- [ ] Add real-time notifications with WebSocket
- [ ] Implement advanced analytics and reporting

---

## 👨‍💻 Developer Information

**Name**: [Your Name]  
**Position**: Junior Java Developer Candidate  
**Email**: [Your Email]  
**LinkedIn**: [Your LinkedIn]  
**GitHub**: [Your GitHub]

---

## 📄 License

This project is developed for educational and portfolio purposes.

---

## 🙏 Acknowledgments

- **Axon Framework** for CQRS/ES implementation
- **Spring Boot** ecosystem for rapid development
- **LangChain** for AI integration capabilities
- **Docker** for containerization simplicity

---

