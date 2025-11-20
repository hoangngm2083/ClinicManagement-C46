# 🔍 DEBUG LOGGING - ExaminationService Integration

**Ngày**: November 20, 2025  
**Vấn đề**: `throw new ResourceNotFoundException("Hồ sơ của bệnh nhân")` được throw khi `examination()` isEmpty  
**Nguyên nhân**: ExaminationService không trả về examination data hoặc query thất bại

---

## 📋 Logging Flow - Chi tiết tracking

### 1️⃣ ExamWorkFlowProcessingSaga.on(TakeNextItemRequestedEvent)
```
[SAGA STARTED] New saga instance created
  └─> Event received - queueId={id}, staffId={id}
      └─> Saga state initialized: TAKE_ITEM_REQUEST_RECEIVED
          └─> Querying for top item in queue: queueId={id}
              └─> Top item found: queueItemId={id}
                  └─> Associated saga with queueItemId={id}
                      └─> Sending TakeNextItemCommand
                          └─> TakeNextItemCommand executed successfully
                              └─> State changed to PENDING_DEQUEUE
```

### 2️⃣ ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)
```
[START] Processing queue item taken event
  └─> queueItemId={id}, staffId={id}, queueId={id}
      └─> State changed to PENDING_SEND_ITEM
          └─> Getting queue item details for queueItemId={id}
              └─> [getQueueItem] Queue item retrieved: serviceId={id}, medicalFormId={id}
                  └─> Getting medical form details for medicalFormId={id}
                      └─> [getMedicalFormDetails] START
```

### 3️⃣ ExamWorkFlowProcessingSaga.getMedicalFormDetails(medicalFormId)
```
[START] Retrieving medical form details for medicalFormId={id}
  └─> Sending query to QueryGateway for medicalFormId={id}
      └─> [MedicalFormQueryHandler] receives GetMedicalFormDetailsByIdQuery
          └─> Medical form view found: id={id}, patientId={id}, examinationId={id}, status={status}
              └─> Querying Patient: patientId={id}
              │   └─> [SUCCESS] Patient retrieved: patientId={id}, name={name}
              │       OR
              │       └─> [WARN] FAILED to retrieve Patient data or Patient is NULL
              │
              └─> Querying Examination: examinationId={id}
                  └─> [SUCCESS] Examination retrieved: examinationId={id}, patientId={id}
                      OR
                      └─> ⚠️ [WARN] FAILED to retrieve Examination data
                          OR
                          └─> ⚠️ [WARN] Examination is NULL from query gateway
                              (THIS IS WHERE ERROR OCCURS)
```

### 4️⃣ 如果Examination为NULL或异常
```
[ExamWorkFlowProcessingSaga.getMedicalFormDetails] ERROR: examination is EMPTY
  └─> This likely means:
      1. ExaminationService did not create exam for this medical form
      2. ExaminationService returned NULL from GetExaminationByIdQuery
      3. Exam creation failed silently in previous step
      
  └─> throw new ResourceNotFoundException("Hồ sơ của bệnh nhân")
```

---

## 🔍 Key Debug Points - Nơi cần kiểm tra

### Point 1: MedicalFormQueryHandler.handle()
```log
[MedicalFormQueryHandler.handle] Querying Examination: examinationId=xxx
[MedicalFormQueryHandler.handle] FAILED to retrieve Examination data for examinationId=xxx
  OR
[MedicalFormQueryHandler.handle] Examination is NULL from query gateway for examinationId=xxx
```

**Nguyên nhân có thể**:
- ExaminationService không nhận GetExaminationByIdQuery
- Query timeout hoặc thất bại
- ExaminationService trả về null
- Exam chưa được tạo

### Point 2: GetExaminationByIdQuery Handler
```log
[ExaminationQueryHandler.handle(GetExaminationByIdQuery)] Query received for examinationId=xxx
[ExaminationQueryHandler.handle] Exam view found: id=xxx, patientId=xxx
  OR
[ExaminationQueryHandler.handle] Exam view NOT found for examinationId=xxx
  OR
[ExaminationQueryHandler.handle] Eager load results failed
```

**Nguyên nhân có thể**:
- ExamView chưa được lưu vào database
- medicalFormId chưa được populate
- Projection chưa chạy

---

## 📊 Expected Log Output (Success Case)

```
[ExamWorkFlowProcessingSaga.on(TakeNextItemRequestedEvent)] SAGA STARTED: New saga instance created
[ExamWorkFlowProcessingSaga.on(TakeNextItemRequestedEvent)] Event received - queueId=QUEUE-001, staffId=STAFF-001
[ExamWorkFlowProcessingSaga.on(TakeNextItemRequestedEvent)] Saga state initialized
[ExamWorkFlowProcessingSaga.on(TakeNextItemRequestedEvent)] Querying for top item in queue: queueId=QUEUE-001
[ExamWorkFlowProcessingSaga.on(TakeNextItemRequestedEvent)] Top item found in queue: queueItemId=ITEM-001
[ExamWorkFlowProcessingSaga.on(TakeNextItemRequestedEvent)] Associated saga with queueItemId=ITEM-001
[ExamWorkFlowProcessingSaga.on(TakeNextItemRequestedEvent)] Sending TakeNextItemCommand: itemId=ITEM-001, staffId=STAFF-001
[ExamWorkFlowProcessingSaga.on(TakeNextItemRequestedEvent)] TakeNextItemCommand executed successfully
[ExamWorkFlowProcessingSaga.on(TakeNextItemRequestedEvent)] State changed to PENDING_DEQUEUE

[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] START: Processing queue item taken event
[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] queueItemId=ITEM-001, staffId=STAFF-001, queueId=QUEUE-001
[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] State changed to PENDING_SEND_ITEM
[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] Getting queue item details for queueItemId=ITEM-001
[ExamWorkFlowProcessingSaga.getQueueItem] SUCCESS: Queue item retrieved - serviceId=SERVICE-001, medicalFormId=FORM-001
[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] Queue item retrieved: serviceId=SERVICE-001, medicalFormId=FORM-001

[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] Getting medical form details for medicalFormId=FORM-001
[ExamWorkFlowProcessingSaga.getMedicalFormDetails] START: Retrieving medical form details for medicalFormId=FORM-001
[ExamWorkFlowProcessingSaga.getMedicalFormDetails] Sending query to QueryGateway for medicalFormId=FORM-001

[MedicalFormQueryHandler.handle] START: Getting medical form details for medicalFormId=FORM-001
[MedicalFormQueryHandler.handle] Medical form view found: id=FORM-001, patientId=PATIENT-001, examinationId=EXAM-001, status=CREATED
[MedicalFormQueryHandler.handle] Querying Patient: patientId=PATIENT-001
[MedicalFormQueryHandler.handle] Patient retrieved successfully: patientId=PATIENT-001, name=John Doe
[MedicalFormQueryHandler.handle] Querying Examination: examinationId=EXAM-001

[ExaminationQueryHandler.handle(GetExaminationByIdQuery)] Query received for examinationId=EXAM-001
[ExaminationQueryHandler.handle] Exam view found: id=EXAM-001, patientId=PATIENT-001
[ExaminationQueryHandler.handle] Eager load results with findByIdWithResults
[ExaminationQueryHandler.handle] Examination retrieved successfully with results
[ExaminationQueryHandler.handle] Returning ExamDetailsDto

[MedicalFormQueryHandler.handle] Examination retrieved successfully: examinationId=EXAM-001, patientId=PATIENT-001
[MedicalFormQueryHandler.handle] Combining results for form=FORM-001: patient=true, examination=true
[MedicalFormQueryHandler.handle] SUCCESS: Form details DTO created for form=FORM-001, hasPatient=true, hasExamination=true

[ExamWorkFlowProcessingSaga.getMedicalFormDetails] Medical form view received: id=FORM-001, hasPatient=true, hasExamination=true
[ExamWorkFlowProcessingSaga.getMedicalFormDetails] Patient info found: patientId=PATIENT-001, name=John Doe
[ExamWorkFlowProcessingSaga.getMedicalFormDetails] Examination info found: examinationId=EXAM-001, patientId=PATIENT-001
[ExamWorkFlowProcessingSaga.getMedicalFormDetails] SUCCESS: All medical form details retrieved

[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] Medical form details retrieved successfully
[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] Getting service details for serviceId=SERVICE-001
[ExamWorkFlowProcessingSaga.getService] SUCCESS: Service retrieved - name=Blood Test
[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] Service details retrieved: serviceName=Blood Test
[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] Building QueueItemResponse
[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] QueueItemResponse built successfully
[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] ======== Sending Queue Item to staff: STAFF-001 ========
[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] Queue item sent to staff via WebSocket
[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] Broadcasting queue size for queueId=QUEUE-001
[ExamWorkFlowProcessingSaga.getQueueSize] SUCCESS: Queue size is 5 for queueId=QUEUE-001
[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] Queue size broadcasted
[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] State changed to ITEM_SENT. SAGA FLOW COMPLETED SUCCESSFULLY
```

---

## 📊 Expected Log Output (Error Case - examination isEmpty)

```
... [previous logs up to querying Examination]

[MedicalFormQueryHandler.handle] Querying Examination: examinationId=EXAM-001
[MedicalFormQueryHandler.handle] FAILED to retrieve Examination data for examinationId=EXAM-001: Query timeout
    OR
[MedicalFormQueryHandler.handle] Examination is NULL from query gateway for examinationId=EXAM-001

[MedicalFormQueryHandler.handle] Combining results for form=FORM-001: patient=true, examination=false
[MedicalFormQueryHandler.handle] SUCCESS: Form details DTO created for form=FORM-001, hasPatient=true, hasExamination=false

[ExamWorkFlowProcessingSaga.getMedicalFormDetails] Medical form view received: id=FORM-001, hasPatient=true, hasExamination=false
[ExamWorkFlowProcessingSaga.getMedicalFormDetails] Patient info found: patientId=PATIENT-001, name=John Doe

[ExamWorkFlowProcessingSaga.getMedicalFormDetails] ERROR: examination is EMPTY for medical form: medicalFormId=FORM-001, form.id=FORM-001, patientId=PATIENT-001
[ExamWorkFlowProcessingSaga.getMedicalFormDetails] This likely means:
  1. ExaminationService did not create exam for this medical form
  2. ExaminationService returned NULL from GetExaminationByIdQuery
  3. Exam creation failed silently in previous step

[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] EXCEPTION occurred: ResourceNotFoundException
[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] Exception message: Hồ sơ của bệnh nhân
[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] Full exception: ... stack trace ...

[ExamWorkFlowProcessingSaga.handleException] Exception occurred in state PENDING_SEND_ITEM: Hồ sơ của bệnh nhân
[ExamWorkFlowProcessingSaga.handleException] Exception class: com.clinic.c46.CommonService.exception.ResourceNotFoundException
[ExamWorkFlowProcessingSaga.handleException] Full stack trace: ... detailed stack trace ...
[ExamWorkFlowProcessingSaga.handleException] Notifying staff STAFF-001 about error: Hồ sơ của bệnh nhân
[ExamWorkFlowProcessingSaga.handleException] Ending saga
```

---

## 🔧 Troubleshooting Steps

### Step 1: Check ExaminationService is running
```
Verify that ExaminationService is running and responding to queries
```

### Step 2: Check if exam was created
```
Look for logs like:
[ExaminationViewProjection.on(ExaminationCreatedEvent)] START: examinationId=EXAM-001
```

### Step 3: Check database
```
SELECT * FROM exam_view WHERE id = 'EXAM-001';
```

If exam is not in database, then projection failed.

### Step 4: Check GetExaminationByIdQuery handler
```
Look for logs like:
[ExaminationQueryHandler.handle(GetExaminationByIdQuery)] Query received for examinationId=EXAM-001
```

If this log doesn't appear, query is not reaching the handler.

### Step 5: Check patient data injection
```
Look for logs like:
[ExaminationViewProjection.on(ExaminationCreatedEvent)] Patient retrieved successfully: patientId={id}, name={name}

If it says "patient-not-found", PatientService is not responding.
```

---

## 📝 Summary

**The logging provides complete visibility into**:
1. ✅ Saga flow from start to finish
2. ✅ Each query step and its success/failure
3. ✅ Data population status (patient, examination)
4. ✅ Exact point where exception occurs
5. ✅ Root cause indicators

**To debug the "Hồ sơ của bệnh nhân" error**:
1. Look for `[MedicalFormQueryHandler.handle] FAILED to retrieve Examination`
2. Or look for `[MedicalFormQueryHandler.handle] Examination is NULL`
3. This will tell you why examination is empty

**Files modified with logging**:
- ✅ ExamWorkFlowProcessingSaga.java (complete flow tracking)
- ✅ MedicalFormQueryHandler.java (query handler tracking)
- ✅ ExaminationDto.java (DTO structure update)

