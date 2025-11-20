## Examination Service - Optimized Query & Result Management

### Thay đổi chính đã thực hiện:

#### 1. **Tối ưu UpdateResultStatusCommand**
- Thêm validation: Chỉ cho phép update result khi status != SIGNED
- Chuyển đổi String newStatus thành enum ResultStatus
- File: `ExaminationAggregate.java`

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

#### 2. **Tạo ExamDetailsDto**
- Record mới chứa đầy đủ thông tin exam + list medical results
- Dùng cho GetExaminationById endpoint
- File: `ExamDetailsDto.java`

```java
@Builder
public record ExamDetailsDto(
        String id,
        String patientId,
        String patientName,
        String patientEmail,
        String medicalFormId,
        List<MedicalResultViewDto> results
) {
}
```

#### 3. **Tối ưu ExamView Entity**
- Thêm field `medicalFormId`
- Giữ `results` với FetchType.LAZY để tránh eager load không cần thiết khi search
- File: `ExamView.java`

#### 4. **ExaminationQueryHandler - Query Optimization**

**SearchExamsQuery handler:**
- Trả về `ExamsPagedDto` với `ExamViewDto` (KHÔNG include results)
- Vận dụng LAZY loading để không load results không cần thiết

**GetExaminationByIdQuery handler:**
- Trả về `Optional<ExamDetailsDto>` với đầy đủ results
- Sử dụng `findByIdWithResults()` để eager load results bằng LEFT JOIN FETCH
- Mapper tự động filter results có status = REMOVED

```java
@QueryHandler
public Optional<ExamDetailsDto> handle(GetExaminationByIdQuery query) {
    return examViewRepository.findByIdWithResults(query.examinationId())
            .map(examMapper::toExamDetailsDto);
}
```

#### 5. **ExamViewRepository - Custom Query**
- Thêm method `findByIdWithResults()` với eager loading
- Sử dụng LEFT JOIN FETCH để load results khi findById

```java
@Query("SELECT DISTINCT e FROM ExamView e LEFT JOIN FETCH e.results WHERE e.id = :examId")
Optional<ExamView> findByIdWithResults(@Param("examId") String examId);
```

#### 6. **ExamMapper - Projector/Converter**
- Chuyển ExamView → ExamDetailsDto
- Tự động filter REMOVED results
- Chuyển ResultStatus enum → String name

```java
public ExamDetailsDto toExamDetailsDto(ExamView examView) {
    List<MedicalResultViewDto> resultDtos = examView.getResults().stream()
            .filter(r -> !r.getStatus().equals(ResultStatus.REMOVED))
            .map(this::toMedicalResultViewDto)
            .toList();
    // ... build DTO
}
```

#### 7. **ExaminationViewProjection - Event Handler Enhancement**
- Khi ExaminationCreatedEvent: gọi GetPatientByIdQuery qua QueryGateway
- Inject patient name, email vào ExamView
- Populate medicalFormId từ event

```java
@EventHandler
public void on(ExaminationCreatedEvent event) {
    ExamView view = ExamView.builder()
            .id(event.examinationId())
            .patientId(event.patientId())
            .medicalFormId(event.medicalFormId())
            .build();
    
    GetPatientByIdQuery query = new GetPatientByIdQuery(event.patientId());
    PatientDto patient = queryGateway.query(query, ResponseTypes.instanceOf(PatientDto.class))
            .join();
    
    view.setPatientName(patient.name());
    view.setPatientEmail(patient.email());
    // ...
}
```

#### 8. **CreateExaminationCommand & ExaminationCreatedEvent**
- Thêm field `medicalFormId` 
- Được truyền qua event để project vào view
- File: `CreateExaminationCommand.java`, `ExaminationCreatedEvent.java`

#### 9. **ResultStatusUpdatedEvent**
- Thay đổi từ `String newStatus` → `ResultStatus newStatus`
- Ensure type safety trong aggregate

#### 10. **ExaminationController**
- Thêm endpoint `GET /{examId}` để lấy chi tiết exam + results
- Sử dụng GetExaminationByIdQuery qua QueryGateway

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

### Database Impact

Migration cần thêm column vào `exam_view` table:
```sql
ALTER TABLE exam_view ADD COLUMN medical_form_id VARCHAR(255);
```

### API Endpoints

**Search Examinations** (LAZY load, không có results)
```
GET /examination?keyword=&page=1
Response: ExamsPagedDto {
    content: [ExamViewDto { id, patientId, patientName, patientEmail, medicalFormId }, ...],
    totalElements, totalPages, ...
}
```

**Get Examination Details** (EAGER load, có results trừ REMOVED)
```
GET /examination/{examId}
Response: ExamDetailsDto {
    id, patientId, patientName, patientEmail, medicalFormId,
    results: [MedicalResultViewDto { doctorId, serviceId, data, pdfUrl, status, doctorName }, ...]
}
```

### Key Improvements

✅ **Performance**: LAZY load results chỉ khi cần
✅ **Data Consistency**: Validation SIGNED status không thể update
✅ **Type Safety**: ResultStatus enum thay vì String
✅ **Clean Architecture**: Mapper pattern cho DTO conversion
✅ **Query Optimization**: Explicit eager load với LEFT JOIN FETCH
✅ **Event Sourcing**: Event injection of patient info + medicalFormId
✅ **Filtering**: Auto-exclude REMOVED results

