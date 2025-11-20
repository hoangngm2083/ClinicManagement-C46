# ExaminationService - Quick Reference Guide

## 1. Core Changes Summary

| Component | Change | Impact | File |
|-----------|--------|--------|------|
| **Aggregate** | Add SIGNED status validation | Prevent update of signed results | `ExaminationAggregate.java` |
| **Aggregate** | Convert String → ResultStatus enum | Type safety | `ExaminationAggregate.java` |
| **Event** | Add medicalFormId field | Track form info | `ExaminationCreatedEvent.java` |
| **Command** | Add medicalFormId field | Pass form info | `CreateExaminationCommand.java` |
| **Event** | String → ResultStatus enum | Type safety | `ResultStatusUpdatedEvent.java` |
| **Entity** | Add medicalFormId column | Store form reference | `ExamView.java` |
| **Repository** | Add findByIdWithResults() | Eager load results | `ExamViewRepository.java` |
| **Projection** | Query GetPatientById | Inject patient info | `ExaminationViewProjection.java` |
| **Query Handler** | Add GetExaminationById handler | Return full details | `ExaminationQueryHandler.java` |
| **Mapper** | Create ExamMapper | Filter REMOVED results | `ExamMapper.java` |
| **DTO** | Create ExamDetailsDto | Response for details endpoint | `ExamDetailsDto.java` |
| **Controller** | Add GET /{examId} endpoint | Expose details query | `ExaminationController.java` |
| **Database** | Add medical_form_id column | Store medicalFormId | `V1.1__migration.sql` |

## 2. Behavioral Changes

### Result Status Update Protection
```
┌─────────┬──────────────────┬─────────────────────────┐
│ Status  │ Can Update       │ Action                  │
├─────────┼──────────────────┼─────────────────────────┤
│ CREATED │ ✅ Yes           │ Apply ResultStatusUpdatedEvent |
│ SIGNED  │ ❌ No (silently) │ Ignore command          │
│ REMOVED │ ❌ No (silently) │ Ignore command          │
└─────────┴──────────────────┴─────────────────────────┘
```

### Query Strategy
```
┌─────────────────────────┬───────────────┬──────────────────┐
│ Query                   │ Load Results  │ Filter REMOVED   │
├─────────────────────────┼───────────────┼──────────────────┤
│ SearchExamsQuery        │ LAZY (No)     │ N/A              │
│ GetExaminationByIdQuery │ EAGER (Yes)   │ Yes (at mapper)  │
└─────────────────────────┴───────────────┴──────────────────┘
```

### Data Flow
```
1. CreateExaminationCommand
   ↓
2. ExaminationAggregate processes command
   ↓
3. ExaminationCreatedEvent emitted with medicalFormId
   ↓
4. ExaminationViewProjection receives event
   ↓
5. Query GetPatientById (to get name, email)
   ↓
6. Build ExamView with patient info + medicalFormId
   ↓
7. Store in database
   ↓
8. Client queries GetExaminationById
   ↓
9. ExamViewRepository.findByIdWithResults() eager loads
   ↓
10. ExamMapper filters REMOVED, converts to ExamDetailsDto
    ↓
11. Return to client
```

## 3. File Organization

```
ExaminationService/
├── src/main/java/
│   └── com/clinic/c46/ExaminationService/
│       ├── domain/
│       │   ├── aggregate/
│       │   │   └── ExaminationAggregate.java ⭐ MODIFIED
│       │   ├── command/
│       │   │   └── UpdateResultStatusCommand.java
│       │   ├── event/
│       │   │   └── ResultStatusUpdatedEvent.java ⭐ MODIFIED
│       │   ├── query/
│       │   │   ├── SearchExamsQuery.java
│       │   │   └── FindExamByIdQuery.java
│       │   └── valueObject/
│       │       └── ResultStatus.java
│       ├── application/
│       │   ├── dto/
│       │   │   ├── ExamDetailsDto.java ⭐ NEW
│       │   │   ├── ExamViewDto.java
│       │   │   ├── ExamsPagedDto.java
│       │   │   └── MedicalResultViewDto.java
│       │   └── handler/
│       │       └── query/
│       │           └── ExaminationQueryHandler.java ⭐ MODIFIED
│       └── infrastructure/
│           ├── adapter/
│           │   ├── helper/
│           │   │   └── ExamMapper.java ⭐ NEW
│           │   ├── persistence/
│           │   │   ├── repository/
│           │   │   │   └── ExamViewRepository.java ⭐ MODIFIED
│           │   │   ├── view/
│           │   │   │   ├── ExamView.java ⭐ MODIFIED
│           │   │   │   └── ResultView.java
│           │   │   └── projector/
│           │   │       └── ExaminationViewProjection.java ⭐ MODIFIED
│           │   └── web/
│           │       └── controller/
│           │           └── ExaminationController.java ⭐ MODIFIED
│           └── config/
├── src/test/java/
│   └── com/clinic/c46/ExaminationService/
│       ├── domain/aggregate/
│       │   └── ExaminationAggregateTest.java ⭐ NEW
│       └── application/handler/query/
│           └── ExaminationQueryHandlerTest.java ⭐ NEW
├── src/main/resources/
│   ├── db/migration/
│   │   └── V1.1__Add_medical_form_id_to_exam_view.sql ⭐ NEW
│   └── application.properties
└── pom.xml

CommonService/
└── src/main/java/
    └── com/clinic/c46/CommonService/
        ├── command/examination/
        │   └── CreateExaminationCommand.java ⭐ MODIFIED
        └── event/examination/
            └── ExaminationCreatedEvent.java ⭐ MODIFIED
```

⭐ = Modified or New file

## 4. Query Examples

### Example 1: Search Examinations (LAZY Load)
```bash
curl -X GET "http://localhost:8080/examination?keyword=John&page=1"

# Response
{
  "content": [
    {
      "id": "EXAM-001",
      "patientId": "PATIENT-001",
      "medicalFormId": "FORM-001",
      "patientName": "John Doe",
      "patientEmail": "john@example.com"
    }
  ],
  "totalElements": 50,
  "totalPages": 5,
  "currentPage": 1,
  "pageSize": 10
}

# Performance: ~100ms (no results loaded)
```

### Example 2: Get Examination Details (EAGER Load + Filter)
```bash
curl -X GET "http://localhost:8080/examination/EXAM-001"

# Response
{
  "id": "EXAM-001",
  "patientId": "PATIENT-001",
  "patientName": "John Doe",
  "patientEmail": "john@example.com",
  "medicalFormId": "FORM-001",
  "results": [
    {
      "doctorId": "DOCTOR-001",
      "serviceId": "SERVICE-001",
      "data": "{...}",
      "pdfUrl": "https://...",
      "status": "SIGNED",
      "doctorName": "Dr. Smith"
    },
    {
      "doctorId": "DOCTOR-002",
      "serviceId": "SERVICE-002",
      "data": "{...}",
      "pdfUrl": "https://...",
      "status": "CREATED",
      "doctorName": "Dr. Johnson"
    }
  ]
}

# Note: REMOVED results are automatically filtered out
# Performance: ~50ms (results loaded with LEFT JOIN FETCH)
```

### Example 3: Update Result Status (Protected)
```java
// Command: Update CREATED result to SIGNED
UpdateResultStatusCommand cmd = UpdateResultStatusCommand.builder()
    .examId("EXAM-001")
    .serviceId("SERVICE-001")
    .newStatus("SIGNED")
    .build();
// ✅ Allowed: CREATED → SIGNED

// Command: Update SIGNED result to CREATED
UpdateResultStatusCommand cmd = UpdateResultStatusCommand.builder()
    .examId("EXAM-001")
    .serviceId("SERVICE-001")
    .newStatus("CREATED")
    .build();
// ❌ Blocked: SIGNED status cannot be updated
// (Command silently ignored, no event published)
```

## 5. Database Schema

### Before
```sql
CREATE TABLE exam_view (
    id VARCHAR(255) PRIMARY KEY,
    patient_id VARCHAR(255),
    patient_name VARCHAR(255),
    patient_email VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN
);
```

### After
```sql
CREATE TABLE exam_view (
    id VARCHAR(255) PRIMARY KEY,
    patient_id VARCHAR(255),
    patient_name VARCHAR(255),
    patient_email VARCHAR(255),
    medical_form_id VARCHAR(255),  -- NEW
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN
);

CREATE INDEX idx_exam_view_medical_form_id ON exam_view(medical_form_id);
```

## 6. Spring Boot Configuration

### Required Dependencies (pom.xml)
```xml
<!-- Already present in project -->
<dependency>
    <groupId>org.axonframework</groupId>
    <artifactId>axon-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

### Application Properties (Optional Tuning)
```properties
# Lazy loading configuration
spring.jpa.properties.hibernate.enable_lazy_load_no_trans=true

# Query timeout
spring.jpa.properties.hibernate.jdbc.fetch_size=1000

# Batch size for queries
spring.jpa.properties.hibernate.jdbc.batch_size=20
```

## 7. Deployment Checklist

- [ ] Build project: `mvn clean package`
- [ ] Run database migration: `V1.1__Add_medical_form_id_to_exam_view.sql`
- [ ] Verify medical_form_id column exists: `SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='exam_view'`
- [ ] Deploy JAR: `java -jar ExaminationService-0.0.1-SNAPSHOT.jar`
- [ ] Test search endpoint: `GET /examination?page=1`
- [ ] Test details endpoint: `GET /examination/{examId}`
- [ ] Monitor logs for "patient-not-found" warnings
- [ ] Verify SIGNED status protection: Try updating SIGNED result (should be silently ignored)

## 8. Known Limitations & Future Improvements

### Current Limitations
1. SIGNED status is immutable (by design)
2. REMOVED results are filtered at mapper level (not database query)
3. Patient info is cached in ExamView (updates not automatic)

### Future Improvements
1. Add batch operations for multiple results
2. Implement soft delete for auditing
3. Cache patient info or use read replicas
4. Add result versioning
5. Implement approval workflow for SIGNED results

## 9. Support & Troubleshooting

### Common Issues

**Q: Why is my result update ignored?**
A: Check if result status is SIGNED. SIGNED results cannot be updated.

**Q: Why are REMOVED results still showing?**
A: Ensure you're using GetExaminationById (not SearchExams). SearchExams doesn't load results at all.

**Q: Performance is slow on SearchExams?**
A: Results are LAZY loaded. If you're accessing results, consider using GetExaminationById instead for a single query.

**Q: Patient name is null?**
A: PatientService may be down or patient not found. Check logs for warnings.

For more help, contact: dev-team@clinic.com

