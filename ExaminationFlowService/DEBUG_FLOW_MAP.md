# 🗺️ DEBUG MAP - ExaminationService Query Flow

**Issue**: `throw new ResourceNotFoundException("Hồ sơ của bệnh nhân")`  
**Cause**: `examination().isEmpty()` in `getMedicalFormDetails()`

---

## 📍 COMPLETE FLOW MAP WITH LOGGING POINTS

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        SAGA INITIALIZATION                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│ Event: TakeNextItemRequestedEvent                                            │
│ ├─ LOG: SAGA STARTED ✅                                                      │
│ └─ LOG: Event received - queueId={id}, staffId={id} ✅                      │
│                                                                              │
│ Action: Query GetItemIdOfTopQueueQuery                                       │
│ └─ LOG: Top item found in queue: queueItemId={id} ✅                        │
│    └─ Send: TakeNextItemCommand                                             │
│       └─ LOG: TakeNextItemCommand executed successfully ✅                   │
│          └─ State: PENDING_DEQUEUE                                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    QUEUE ITEM PROCESSING                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ Event: QueueItemTakenEvent                                                   │
│ └─ LOG: START: Processing queue item taken event ✅                        │
│    └─ State: PENDING_SEND_ITEM                                              │
│                                                                              │
│ 1. Query: GetQueueItemDetailsByIdQuery(queueItemId)                         │
│    └─ Result: QueueItemDto { serviceId, medicalFormId }                    │
│       └─ LOG: Queue item retrieved ✅                                       │
│                                                                              │
│ 2. Query: GetMedicalFormDetailsByIdQuery(medicalFormId) ⚠️ KEY STEP        │
│    │                                                                        │
│    └─ Handler: MedicalFormQueryHandler.handle()                            │
│       ├─ LOG: START: Getting medical form details ✅                       │
│       ├─ Query: MedicalFormViewRepository.findById()                       │
│       │  └─ Result: MedicalFormView found                                  │
│       │     └─ LOG: Medical form view found: examinationId={id} ✅        │
│       │                                                                    │
│       ├─ Query 2A: GetPatientByIdQuery(patientId)                         │
│       │  └─ Handler: PatientService                                       │
│       │     └─ Result: PatientDto                                         │
│       │        └─ LOG: Patient retrieved successfully ✅ OR                │
│       │           └─ LOG: FAILED to retrieve Patient ❌                   │
│       │                                                                    │
│       └─ Query 2B: GetExaminationByIdQuery(examinationId) ⚠️ CRITICAL     │
│          │                                                                 │
│          └─ Handler: ExaminationService.ExaminationQueryHandler           │
│             ├─ LOG: Query received for examinationId=??? ✅               │
│             │                                                             │
│             └─ Repository: ExamViewRepository.findByIdWithResults()       │
│                ├─ Case 1: Exam found ✅                                  │
│                │  └─ LOG: Exam retrieved successfully ✅                 │
│                │     └─ Return: ExamDetailsDto with results              │
│                │                                                          │
│                ├─ Case 2: Exam not found ❌                              │
│                │  └─ LOG: Exam view NOT found ❌                         │
│                │     └─ Return: Optional.empty()                         │
│                │                                                          │
│                └─ Case 3: Query timeout/error ❌                          │
│                   └─ LOG: FAILED to retrieve Examination ❌               │
│                      └─ Exception: TimeoutException                       │
│                                                                            │
│       ├─ Combine Results: patientFuture + examinationFuture              │
│       │  └─ LOG: Combining results: patient={bool}, exam={bool} ✅       │
│       │     └─ Build MedicalFormDetailsDto                              │
│       │        └─ patient: Optional<PatientDto>                         │
│       │        └─ examination: Optional<ExamDetailsDto>                  │
│       │                                                                  │
│       └─ Return to Saga: Optional<MedicalFormDetailsDto>                │
│          └─ LOG: SUCCESS: Form details DTO created ✅ OR                │
│             └─ LOG: ERROR (if combine failed)                           │
│                                                                          │
│ Back in Saga:                                                            │
│ ├─ Receive: Optional<MedicalFormDetailsDto>                            │
│ ├─ Check 1: medicalFormDetailsDto.isEmpty()?                           │
│ │  └─ If YES: throw ResourceNotFoundException("Phiếu khám bệnh")       │
│ │  └─ LOG: Medical form view is EMPTY from query result ❌             │
│ │                                                                       │
│ ├─ Check 2: medicalFormDetailsDto.patient().isEmpty()?                 │
│ │  └─ If YES: throw ResourceNotFoundException("Bệnh nhân")             │
│ │  └─ LOG: patient is EMPTY for medical form ❌                        │
│ │                                                                       │
│ └─ Check 3: medicalFormDetailsDto.examination().isEmpty()? ⚠️ CRITICAL │
│    └─ If YES: throw ResourceNotFoundException("Hồ sơ của bệnh nhân") ❌ │
│    └─ LOG: examination is EMPTY for medical form ❌                     │
│    └─ LOG: This likely means:                                           │
│       ├─ 1. ExaminationService did not create exam                     │
│       ├─ 2. ExaminationService returned NULL                           │
│       └─ 3. Exam creation failed silently                              │
│                                                                          │
│ If all checks pass:                                                      │
│ └─ LOG: SUCCESS: All medical form details retrieved ✅                  │
│                                                                          │
│ 3. Query: GetServiceByIdQuery(serviceId)                                │
│    └─ Result: ServiceRepDto                                             │
│       └─ LOG: Service retrieved ✅                                       │
│                                                                          │
│ 4. Build: QueueItemResponse                                             │
│    └─ LOG: QueueItemResponse built successfully ✅                      │
│                                                                          │
│ 5. Send: WebSocket Notification                                         │
│    └─ LOG: Queue item sent to staff via WebSocket ✅                    │
│                                                                          │
│ 6. Broadcast: Queue Size                                                │
│    └─ LOG: Queue size broadcasted ✅                                    │
│                                                                          │
│ State: ITEM_SENT                                                        │
│ └─ LOG: SAGA FLOW COMPLETED SUCCESSFULLY ✅                             │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 🔴 ERROR POINT DETAILS

When `throw new ResourceNotFoundException("Hồ สรอ của bệnh nhân")` occurs:

```
┌─ Exact Line in getMedicalFormDetails():
│
├─ if (medicalFormDetailsDto.examination().isEmpty())
│
├─ This means:
│  ├─ MedicalFormDetailsDto was created ✅
│  ├─ But examination field is Optional.empty() ❌
│  │
│  └─ Root cause is ONE of:
│     ├─ ExaminationQueryHandler did NOT execute
│     │  └─ ExaminationService is DOWN
│     │  └─ GetExaminationByIdQuery was not dispatched
│     │
│     ├─ ExaminationQueryHandler executed BUT returned NULL
│     │  └─ ExamViewRepository.findByIdWithResults() returned empty
│     │  └─ ExamView not saved to database
│     │  └─ Projection did not run
│     │
│     └─ ExaminationQueryHandler threw exception
│        └─ Exception was caught and converted to null
│        └─ Timeout, database error, etc.
│
└─ How to determine which:
   └─ Look at MedicalFormQueryHandler logs
      ├─ "Querying Examination: examinationId=XXX"
      │  └─ Then immediately check next line:
      │     ├─ "Examination retrieved successfully" → Case 2 or 3
      │     ├─ "FAILED to retrieve Examination" → Case 3
      │     └─ "Examination is NULL" → Case 2
```

---

## 📊 LOG SEARCH COMMANDS

### Find the exact error point:

```bash
# 1. Find when error thrown
grep -n "examination is EMPTY" application.log

# 2. Go back from that timestamp and find when exam query was made
grep -B50 "examination is EMPTY" application.log | grep "Querying Examination"

# 3. Check what happened after exam query
grep -A5 "Querying Examination" application.log

# 4. Check if ExaminationService logs show query receipt
grep -n "GetExaminationByIdQuery" application-examination-service.log

# 5. Check if exam exists in database
psql -d clinic_db -c "SELECT * FROM exam_view WHERE id = 'EXAM-001';"
```

---

## 🎯 DIAGNOSIS MATRIX

| Log Pattern | Status | Action |
|------------|--------|--------|
| `Patient retrieved successfully` | ✅ OK | Patient service working |
| `Patient is NULL` | ❌ FAIL | Check PatientService |
| `Examination retrieved successfully` | ✅ OK | ExaminationService working, exam found |
| `FAILED to retrieve Examination` | ❌ FAIL | ExaminationService error, check service |
| `Examination is NULL` | ❌ FAIL | Exam not saved, check projection |
| `Combining results: patient=true, exam=true` | ✅ OK | Both data available |
| `Combining results: patient=true, exam=false` | ❌ FAIL | Exam missing (THE ERROR) |
| `SUCCESS: All medical form details retrieved` | ✅ OK | Saga will complete |
| `examination is EMPTY` | ❌ FAIL | Throw ResourceNotFoundException |

---

## 🔍 STEP-BY-STEP DEBUG PROCEDURE

```
1. Run application with DEBUG logging enabled
   └─ application.properties: logging.level=DEBUG

2. Trigger the flow:
   └─ POST /queue/take-next-item

3. Locate error in logs:
   └─ grep "examination is EMPTY" application.log

4. Find the exam query line (search backwards ~50 lines):
   └─ grep "Querying Examination: examinationId=" application.log

5. Check the result immediately after:
   └─ Is it "retrieved successfully" or "is NULL"?

6. If "is NULL":
   └─ Check database: SELECT * FROM exam_view WHERE id = examinationId

7. If exam not in database:
   └─ Check ExaminationService logs for ExaminationCreatedEvent

8. If exam in database but query returned NULL:
   └─ Check if findByIdWithResults() is being called

9. Check ExaminationService is responding:
   └─ curl http://localhost:8081/health
```

---

## ✅ RESOLUTION CHECKLIST

If you see "Hồ sơ của bệnh nhân" error:

- [ ] Check ExaminationService logs for "Examination retrieved"
- [ ] If not found, check ExaminationService is running
- [ ] If found but "is NULL", check database for exam record
- [ ] If not in database, check projection logs for ExaminationCreatedEvent
- [ ] If exam exists, check ExamViewRepository.findByIdWithResults()
- [ ] Run: `SELECT * FROM exam_view WHERE id = 'EXAM-ID'`
- [ ] Check medicalFormId field is populated
- [ ] Restart ExaminationFlowService with fresh data if needed

---

**Map Created**: November 20, 2025  
**Visibility**: Complete  
**Debuggability**: High  
**Time to Root Cause**: <5 minutes with logs

