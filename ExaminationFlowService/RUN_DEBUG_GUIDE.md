# 🚀 HOW TO DEBUG - ExamWorkFlowProcessingSaga Exception

**Vấn đề**: `throw new ResourceNotFoundException("Hồ sơ của bệnh nhân")`  
**Nguyên nhân**: `medicalFormDetailsDto.examination().isEmpty()`  
**Giải pháp**: Theo dõi logs chi tiết ở mỗi bước

---

## 1️⃣ Chuẩn bị môi trường

### Bật DEBUG logging

Thêm vào `application.properties` hoặc `application-dev.properties`:

```properties
# ExaminationFlowService
logging.level.com.clinic.c46.ExaminationFlowService.application.saga=DEBUG
logging.level.com.clinic.c46.ExaminationFlowService.infrastructure.adapter.query=DEBUG

# ExaminationService (nếu chạy cùng process)
logging.level.com.clinic.c46.ExaminationService.application.handler.query=DEBUG
logging.level.com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.projector=DEBUG

# Axon Framework
logging.level.org.axonframework=WARN
logging.level.org.axonframework.queryhandling=DEBUG
logging.level.org.axonframework.eventhandling=DEBUG
```

### Hoặc tạo logback file override

`src/main/resources/logback-spring.xml`:

```xml
<configuration>
    <!-- ... existing config ... -->
    
    <logger name="com.clinic.c46.ExaminationFlowService" level="DEBUG"/>
    <logger name="com.clinic.c46.ExaminationService" level="DEBUG"/>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

---

## 2️⃣ Chạy Test/Application

### Option A: Chạy với test

```bash
cd ExaminationFlowService
mvn test -Dtest=ExamWorkFlowProcessingSagaTest
```

### Option B: Chạy application

```bash
cd ExaminationFlowService
mvn spring-boot:run
```

### Option C: Debug mode trong IDE

1. Set breakpoint ở `ExamWorkFlowProcessingSaga.getMedicalFormDetails()` dòng examine isEmpty()
2. Run với Debug mode
3. Step through execution

---

## 3️⃣ Trigger test case

### Tạo test data

1. Tạo Patient:
```bash
POST /patient
Body: { "name": "John Doe", "email": "john@example.com" }
Response: { "id": "PATIENT-001" }
```

2. Tạo Medical Form:
```bash
POST /medical-form
Body: { "patientId": "PATIENT-001", "medicalFormStatus": "CREATED" }
Response: { "id": "FORM-001" }
```

3. Check ExamView được tạo:
```sql
SELECT * FROM exam_view WHERE id = (
  SELECT examination_id FROM medical_form_view WHERE id = 'FORM-001'
);
```

4. Trigger saga:
```bash
POST /queue/take-next-item
Body: { "queueId": "QUEUE-001", "staffId": "STAFF-001" }
```

---

## 4️⃣ Đọc logs theo thứ tự

### Log Sequence để Follow:

1. **Saga Start**
```
[ExamWorkFlowProcessingSaga.on(TakeNextItemRequestedEvent)] SAGA STARTED
```

2. **Queue Item Taken**
```
[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] START: Processing queue item taken event
```

3. **Getting Medical Form Details** ⚠️ KEY POINT
```
[ExamWorkFlowProcessingSaga.getMedicalFormDetails] START: Retrieving medical form details for medicalFormId=FORM-001
└─> [MedicalFormQueryHandler.handle] START: Getting medical form details
    └─> Medical form view found: id=FORM-001, examinationId=EXAM-001
        └─> Querying Examination: examinationId=EXAM-001
```

4. **Check Examination Query Result** ⚠️ THIS IS WHERE ERROR HAPPENS
```
Nếu thấy logs như:
[MedicalFormQueryHandler.handle] Examination retrieved successfully: examinationId=EXAM-001
  → Exam được trả về OK

Nếu thấy logs như:
[MedicalFormQueryHandler.handle] FAILED to retrieve Examination data for examinationId=EXAM-001: 
  → ExaminationService query failed

Hoặc:
[MedicalFormQueryHandler.handle] Examination is NULL from query gateway for examinationId=EXAM-001
  → ExaminationService trả về null
```

5. **Result Combination**
```
[MedicalFormQueryHandler.handle] Combining results: patient=true, examination=???
```

6. **Error Detection**
```
[ExamWorkFlowProcessingSaga.getMedicalFormDetails] ERROR: examination is EMPTY
└─> throw ResourceNotFoundException("Hồ sơ của bệnh nhân")
```

---

## 5️⃣ Interpretation Guide

### Scenario 1: SUCCESS (examination=true)
```
[MedicalFormQueryHandler.handle] Examination retrieved successfully: examinationId=EXAM-001, patientId=PATIENT-001
[MedicalFormQueryHandler.handle] Combining results for form=FORM-001: patient=true, examination=true
[ExamWorkFlowProcessingSaga.getMedicalFormDetails] Examination info found: examinationId=EXAM-001, patientId=PATIENT-001
[ExamWorkFlowProcessingSaga.getMedicalFormDetails] SUCCESS: All medical form details retrieved

✅ SAGA COMPLETES SUCCESSFULLY
```

### Scenario 2: EXAM NOT CREATED
```
[MedicalFormQueryHandler.handle] Medical form view found: examinationId=null
[MedicalFormQueryHandler.handle] Querying Examination: examinationId=null

❌ EXAM WAS NEVER CREATED
Check: ExaminationCreatedEvent was not triggered
```

### Scenario 3: EXAM CREATED BUT QUERY FAILED
```
[MedicalFormQueryHandler.handle] Querying Examination: examinationId=EXAM-001
[MedicalFormQueryHandler.handle] FAILED to retrieve Examination data for examinationId=EXAM-001: 
  java.util.concurrent.TimeoutException

❌ EXAMINATION SERVICE TIMEOUT
Check: ExaminationService is running?
```

### Scenario 4: EXAM CREATED BUT RETURNS NULL
```
[MedicalFormQueryHandler.handle] Querying Examination: examinationId=EXAM-001
[MedicalFormQueryHandler.handle] Examination is NULL from query gateway

❌ EXAM VIEW NOT SAVED TO DATABASE
Check: ExamViewRepository.save() was not called
```

### Scenario 5: PATIENT MISSING
```
[ExamWorkFlowProcessingSaga.getMedicalFormDetails] ERROR: patient is EMPTY

❌ PATIENT NOT FOUND
Check: PatientService availability
```

---

## 6️⃣ Root Cause Checklist

Khi thấy `ResourceNotFoundException("Hồ sơ của bệnh nhân")`, check theo thứ tự:

- [ ] **Exam View Created?**
  ```sql
  SELECT COUNT(*) FROM exam_view;
  ```
  If 0 → ExaminationViewProjection chưa chạy

- [ ] **Exam View has examinationId?**
  ```sql
  SELECT examination_id FROM exam_view WHERE id = 'EXAM-001';
  ```
  If NULL → Projection không set medicalFormId

- [ ] **ExaminationService started?**
  ```
  curl http://localhost:8081/health
  ```
  If error → Service not running

- [ ] **Exam exists in ExaminationService DB?**
  ```sql
  SELECT * FROM exam_view WHERE id = 'EXAM-001';
  ```
  (Trong ExaminationService database)
  If not found → Exam creation failed

- [ ] **GetExaminationByIdQuery handler registered?**
  Look for log:
  ```
  [ExaminationQueryHandler.handle(GetExaminationByIdQuery)]
  ```
  If not found → Handler not active

- [ ] **Query Gateway communication OK?**
  Check for timeout/exception logs in MedicalFormQueryHandler

---

## 7️⃣ Example: Complete Debug Session

```
Terminal 1: Watch ExaminationFlowService logs
tail -f logs/application.log | grep -E "ExamWorkFlow|MedicalFormQuery"

Terminal 2: Make API call
curl -X POST http://localhost:8080/queue/take-next-item \
  -H "Content-Type: application/json" \
  -d '{"queueId":"QUEUE-001","staffId":"STAFF-001"}'

Terminal 3: Check database
psql -d clinic_db -c "SELECT * FROM exam_view WHERE id = 'EXAM-001';"
```

**Expected output sequence in logs**:

```
01:23:45.123 [SAGA STARTED] New saga instance created
01:23:45.234 [START] Processing queue item taken event
01:23:45.345 [START] Retrieving medical form details
01:23:45.456 [MedicalFormQueryHandler] Medical form view found
01:23:45.567 [MedicalFormQueryHandler] Querying Examination
01:23:45.678 [MedicalFormQueryHandler] Examination retrieved successfully  ← SUCCESS
01:23:45.789 [Combining results] patient=true, examination=true
01:23:45.890 [SUCCESS] All medical form details retrieved
01:23:45.901 [ITEM_SENT] SAGA FLOW COMPLETED SUCCESSFULLY ✅
```

---

## 🎯 Summary

**3 steps để debug**:

1. **Bật logging** → application.properties
2. **Trigger event** → POST /queue/take-next-item
3. **Đọc logs** → Tìm dòng "Examination retrieved" hoặc "FAILED to retrieve"

**Nếu thấy**: `Examination is NULL` hoặc `FAILED to retrieve`
→ Problem ở ExaminationService, check GetExaminationByIdQuery handler

**Nếu không thấy log từ ExaminationService**:
→ Service not responding, check network/configuration

**Log files location**:
- ExaminationFlowService: `ExaminationFlowService/logs/application.log`
- ExaminationService: `ExaminationService/logs/application.log`

---

**Debug Guide Created**: November 20, 2025  
**Status**: Ready to use

