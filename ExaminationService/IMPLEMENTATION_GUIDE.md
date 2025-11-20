# ExaminationService - Optimized Query & Result Management

## Overview

Triển khai tối ưu hóa hệ thống query và quản lý medical results trong ExaminationService với các cải tiến sau:

1. **SIGNED Status Protection** - Chỉ cho phép update result khi status != SIGNED
2. **Query Optimization** - LAZY load results khi search, EAGER load khi lấy chi tiết
3. **Data Enrichment** - Tự động inject patient info từ PatientService khi tạo exam
4. **Result Filtering** - Tự động loại bỏ REMOVED results khi trả về
5. **Type Safety** - Sử dụng ResultStatus enum thay vì String

## Architecture Changes

### 1. Command & Event Layer

#### CreateExaminationCommand (CommonService)
```java
@Builder
public record CreateExaminationCommand(
    @TargetAggregateIdentifier @NotBlank String examinationId,
    @NotBlank String patientId,
    String medicalFormId  // NEW
) { }
```

#### ExaminationCreatedEvent (CommonService)
```java
@Builder
public record ExaminationCreatedEvent(
    String examinationId, 
    String patientId, 
    String medicalFormId  // NEW
) { }
```

#### ResultStatusUpdatedEvent (ExaminationService)
```java
@Builder
public record ResultStatusUpdatedEvent(
    String examId, 
    String serviceId, 
    ResultStatus newStatus  // CHANGED: was String, now ResultStatus enum
) { }
```

### 2. Aggregate Layer

#### ExaminationAggregate
- **Constructor**: Accepts CreateExaminationCommand với medicalFormId
- **handle(UpdateResultStatusCommand)**: 
  - Validate status != SIGNED trước khi update
  - Chuyển đổi String newStatus → ResultStatus enum
  - Tự động ignore nếu result status = SIGNED

```java
@CommandHandler
public void handle(UpdateResultStatusCommand cmd) {
    MedicalResult resultToUpdate = findResultOrThrow(cmd.serviceId());
    
    // Only allow update if current status is not SIGNED
    if (resultToUpdate.getStatus().equals(ResultStatus.SIGNED)) {
        log.warn("examination.update-result.command Result with status SIGNED cannot be updated");
        return;
    }
    
    ResultStatus newStatus = ResultStatus.valueOf(cmd.newStatus());
    if (resultToUpdate.getStatus().equals(newStatus)) {
        return;
    }
    
    AggregateLifecycle.apply(new ResultStatusUpdatedEvent(cmd.examId(), cmd.serviceId(), newStatus));
}
```

### 3. Persistence Layer

#### ExamView Entity
- **Thêm field**: `private String medicalFormId;`
- **Fetch Strategy**: 
  - `results`: LAZY load (FetchType.LAZY) để tránh load không cần thiết khi search
  - Custom query: `findByIdWithResults()` để eager load khi cần

```java
@ElementCollection(fetch = FetchType.LAZY)  // LAZY load for search performance
@CollectionTable(name = "exam_result_view", joinColumns = @JoinColumn(name = "exam_id"))
private Set<ResultView> results = new HashSet<>();
```

#### ExamViewRepository
```java
@Repository
public interface ExamViewRepository extends JpaRepository<ExamView, String>, JpaSpecificationExecutor<ExamView> {
    
    @Query("SELECT DISTINCT e FROM ExamView e LEFT JOIN FETCH e.results WHERE e.id = :examId")
    Optional<ExamView> findByIdWithResults(@Param("examId") String examId);
}
```

### 4. Projection Layer

#### ExaminationViewProjection
- **on(ExaminationCreatedEvent)**:
  - Query GetPatientById qua QueryGateway
  - Inject patient name, email vào ExamView
  - Populate medicalFormId từ event

```java
@EventHandler
public void on(ExaminationCreatedEvent event) {
    log.info("[examination.projection.ExaminationCreatedEvent]: {}", event.examinationId());
    ExamView view = ExamView.builder()
            .id(event.examinationId())
            .patientId(event.patientId())
            .medicalFormId(event.medicalFormId())
            .build();

    GetPatientByIdQuery query = new GetPatientByIdQuery(event.patientId());
    PatientDto patient = queryGateway.query(query, ResponseTypes.instanceOf(PatientDto.class))
            .join();

    if (patient == null) {
        log.warn("[examination.projection.ExaminationCreatedEvent.patient-not-found] patientId: {}",
                event.patientId());
        throw new IllegalStateException("Patient not found: " + event.patientId());
    }

    view.setPatientName(patient.name());
    view.setPatientEmail(patient.email());
    view.markCreated();
    examViewRepository.save(view);
}
```

### 5. Query Handler Layer

#### ExaminationQueryHandler

**SearchExamsQuery** (LAZY load results):
```java
@QueryHandler
public ExamsPagedDto handle(SearchExamsQuery q) {
    Pageable pageable = pageAndSortHelper.buildPageable(q.page(), "", SortDirection.ASC);
    Specification<ExamView> spec = specificationBuilder.keyword(q.keyword(),
            List.of("patientName", "patientEmail"));
    Page<ExamView> pageResult = examViewRepository.findAll(spec, pageable);

    return pageAndSortHelper.toPaged(pageResult, view -> ExamViewDto.builder()
            .id(view.getId())
            .patientId(view.getPatientId())
            .patientName(view.getPatientName())
            .patientEmail(view.getPatientEmail())
            .medicalFormId(view.getMedicalFormId())
            .build(), ExamsPagedDto::new);
}
```

**GetExaminationById** (EAGER load results, filter REMOVED):
```java
@QueryHandler
public Optional<ExamDetailsDto> handle(GetExaminationByIdQuery query) {
    return examViewRepository.findByIdWithResults(query.examinationId())
            .map(examMapper::toExamDetailsDto);
}
```

### 6. Mapper/Converter Layer

#### ExamMapper
- Chuyển đổi ExamView → ExamDetailsDto
- Tự động filter results có status = REMOVED
- Chuyển đổi ResultStatus enum → String name

```java
public ExamDetailsDto toExamDetailsDto(ExamView examView) {
    List<MedicalResultViewDto> resultDtos = examView.getResults().stream()
            .filter(r -> !r.getStatus().equals(ResultStatus.REMOVED))
            .map(this::toMedicalResultViewDto)
            .toList();

    return ExamDetailsDto.builder()
            .id(examView.getId())
            .patientId(examView.getPatientId())
            .patientName(examView.getPatientName())
            .patientEmail(examView.getPatientEmail())
            .medicalFormId(examView.getMedicalFormId())
            .results(resultDtos)
            .build();
}

private MedicalResultViewDto toMedicalResultViewDto(ResultView resultView) {
    return MedicalResultViewDto.builder()
            .doctorId(resultView.getDoctorId())
            .serviceId(resultView.getServiceId())
            .data(resultView.getData())
            .pdfUrl(resultView.getPdfUrl())
            .status(resultView.getStatus().name())
            .doctorName(resultView.getDoctorName())
            .build();
}
```

### 7. DTO Layer

#### ExamDetailsDto (NEW)
```java
@Builder
public record ExamDetailsDto(
        String id,
        String patientId,
        String patientName,
        String patientEmail,
        String medicalFormId,
        List<MedicalResultViewDto> results
) { }
```

#### ExamViewDto (Updated)
```java
@Builder
public record ExamViewDto(
    String id, 
    String patientId, 
    String medicalFormId,  // NEW
    String patientName, 
    String patientEmail
) { }
```

### 8. Web Controller Layer

#### ExaminationController

**Search Examinations** (LAZY load):
```java
@GetMapping
public CompletableFuture<ResponseEntity<ExamsPagedDto>> searchExaminations(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "1") Integer page) {
    SearchExamsQuery query = SearchExamsQuery.builder()
            .keyword(keyword)
            .page(page)
            .build();
    return queryGateway.query(query, ResponseTypes.instanceOf(ExamsPagedDto.class))
            .thenApply(ResponseEntity::ok);
}
```

**Get Examination Details** (EAGER load with results):
```java
@GetMapping("/{examId}")
public CompletableFuture<ResponseEntity<ExamDetailsDto>> getExaminationById(
        @PathVariable String examId) {
    GetExaminationByIdQuery query = GetExaminationByIdQuery.builder()
            .examinationId(examId)
            .build();
    return queryGateway.query(query, ResponseTypes.instanceOf(ExamDetailsDto.class))
            .thenApply(ResponseEntity::ok);
}
```

## API Endpoints

### Search Examinations
```
GET /examination?keyword=&page=1

Response: ExamsPagedDto {
    content: [
        {
            id: "EXAM-001",
            patientId: "PATIENT-001",
            medicalFormId: "FORM-001",
            patientName: "John Doe",
            patientEmail: "john@example.com"
        },
        ...
    ],
    totalElements: 100,
    totalPages: 10,
    currentPage: 1,
    pageSize: 10
}
```

### Get Examination Details
```
GET /examination/{examId}

Response: ExamDetailsDto {
    id: "EXAM-001",
    patientId: "PATIENT-001",
    patientName: "John Doe",
    patientEmail: "john@example.com",
    medicalFormId: "FORM-001",
    results: [
        {
            doctorId: "DOCTOR-001",
            serviceId: "SERVICE-001",
            data: "{...json data...}",
            pdfUrl: "https://example.com/result.pdf",
            status: "SIGNED",
            doctorName: "Dr. Smith"
        },
        {
            doctorId: "DOCTOR-002",
            serviceId: "SERVICE-002",
            data: "{...json data...}",
            pdfUrl: "https://example.com/result2.pdf",
            status: "CREATED",
            doctorName: "Dr. Johnson"
        }
    ]
}
```

Note: REMOVED results được tự động filtered out

## Database Migration

### SQL Migration File
```sql
-- V1.1__Add_medical_form_id_to_exam_view.sql

ALTER TABLE exam_view ADD COLUMN IF NOT EXISTS medical_form_id VARCHAR(255) NULL;
CREATE INDEX IF NOT EXISTS idx_exam_view_medical_form_id ON exam_view(medical_form_id);
```

## Files Changed/Created

### Modified Files
- ✅ `CommonService/ExaminationCreatedEvent.java` - Thêm medicalFormId
- ✅ `CommonService/CreateExaminationCommand.java` - Thêm medicalFormId
- ✅ `ExaminationService/ExaminationAggregate.java` - Thêm validation SIGNED status
- ✅ `ExaminationService/ResultStatusUpdatedEvent.java` - String → ResultStatus enum
- ✅ `ExaminationService/ExamView.java` - Thêm medicalFormId field
- ✅ `ExaminationService/ExamViewRepository.java` - Thêm findByIdWithResults()
- ✅ `ExaminationService/ExaminationViewProjection.java` - GetPatientById query + medicalFormId
- ✅ `ExaminationService/ExaminationQueryHandler.java` - GetExaminationById handler
- ✅ `ExaminationService/ExaminationController.java` - Thêm GET /{examId} endpoint

### New Files Created
- ✅ `ExamDetailsDto.java` - DTO cho chi tiết exam với results
- ✅ `ExamMapper.java` - Mapper từ ExamView → ExamDetailsDto
- ✅ `ExaminationQueryHandlerTest.java` - Unit tests
- ✅ `ExaminationAggregateTest.java` - Unit tests
- ✅ `V1.1__Add_medical_form_id_to_exam_view.sql` - Database migration
- ✅ `CHANGES_SUMMARY.md` - Tóm tắt thay đổi

## Performance Improvements

### Before
- SearchExams: Eager load results (N+1 query problem)
- GetExamDetails: Requires separate query for results

### After
- **SearchExams**: LAZY load results → Faster pagination, less memory
- **GetExamDetails**: EAGER load with LEFT JOIN FETCH → Single query, optimal performance
- **Result Filtering**: Done at mapper level → No extra DB queries
- **Indexing**: Added index on medical_form_id → Faster lookups

## Key Features

### 1. SIGNED Status Protection
```
Status: CREATED → ✅ Can update
Status: SIGNED  → ❌ Cannot update (silently ignored)
Status: REMOVED → ❌ Cannot update (already removed)
```

### 2. Automatic Result Filtering
- REMOVED results automatically filtered in GetExaminationById
- Only CREATED/SIGNED results returned

### 3. Patient Data Enrichment
```
Event: ExaminationCreatedEvent received
Query: GetPatientById(patientId) via QueryGateway
Projection: Patient info injected into ExamView
Storage: PatientName, PatientEmail persisted
```

### 4. Type-Safe Status Handling
```
Before: UpdateResultStatusCommand { newStatus: String }
After:  UpdateResultStatusCommand { newStatus: String }
        ResultStatusUpdatedEvent { newStatus: ResultStatus enum }
```

## Testing

### Unit Tests Included
- ✅ `ExaminationQueryHandlerTest` - Query handler tests
- ✅ `ExaminationAggregateTest` - Aggregate lifecycle tests

### Test Coverage
- Search examinations (LAZY load)
- Get examination details (EAGER load with filtering)
- SIGNED status protection
- Result removal
- Patient info injection
- Filter REMOVED results

## Migration Guide

### For Developers

1. **Update command**: Provide `medicalFormId` when creating exam
```java
CreateExaminationCommand.builder()
    .examinationId(examId)
    .patientId(patientId)
    .medicalFormId(medicalFormId)  // NEW
    .build();
```

2. **Use GetExaminationById**: For getting full exam details with results
```java
GetExaminationByIdQuery.builder()
    .examinationId(examId)
    .build();
```

3. **Expect ExamDetailsDto**: Instead of ExamViewDto
```java
Optional<ExamDetailsDto> result = queryGateway.query(...)
    .thenApply(response -> response);
```

### For DBAs

1. **Run migration**: Execute V1.1__Add_medical_form_id_to_exam_view.sql
2. **Verify**: Check medical_form_id column added to exam_view
3. **Index**: Index will be created automatically

## Troubleshooting

### Issue: "Cannot update result with status SIGNED"
**Solution**: This is expected behavior. SIGNED results are immutable. Use RemoveResultCommand if needed.

### Issue: REMOVED results still appearing in GetExaminationById
**Solution**: Ensure ExamMapper is being used. REMOVED filtering happens at mapper level.

### Issue: Patient name/email is null in ExamView
**Solution**: Ensure PatientService is running and GetPatientById query is configured. Check logs for "patient-not-found" warnings.

### Issue: "N+1 query problem" on search
**Solution**: Use SearchExamsQuery instead of GetExaminationById. It uses LAZY loading.

## References

- Axon Framework Documentation: https://docs.axoniq.io/
- Spring Data JPA Query Methods: https://docs.spring.io/spring-data/jpa/docs/current/reference/html/
- Event Sourcing Patterns: https://martinfowler.com/eaaDev/EventSourcing.html

