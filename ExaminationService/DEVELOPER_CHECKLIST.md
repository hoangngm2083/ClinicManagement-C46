# Developer Checklist - ExaminationService 1.1 Update

## Pre-Deployment (Development)

### Code Changes Review
- [ ] Review `ExaminationAggregate.java` - SIGNED status validation
- [ ] Review `ExamViewRepository.java` - findByIdWithResults() query
- [ ] Review `ExaminationViewProjection.java` - GetPatientById query
- [ ] Review `ExaminationQueryHandler.java` - dual query handlers
- [ ] Review `ExamMapper.java` - REMOVED result filtering
- [ ] Review `ExaminationController.java` - new GET /{examId} endpoint

### Build & Compilation
- [ ] Run `mvn clean compile` - No compilation errors
- [ ] Check for warnings: `mvn clean package | grep WARNING`
- [ ] Verify no unused imports
- [ ] Run `mvn dependency:tree` - Check for conflicts

### Unit Testing
- [ ] Run `mvn test` - All tests pass
  ```bash
  mvn test
  # Expected: BUILD SUCCESS
  ```
- [ ] Check coverage: `mvn jacoco:report`
  - Target: >80% coverage
- [ ] Run specific test classes:
  ```bash
  mvn test -Dtest=ExaminationAggregateTest
  mvn test -Dtest=ExaminationQueryHandlerTest
  ```

### Integration Testing
- [ ] Test with CommonService running
- [ ] Verify GetPatientById query works
- [ ] Verify patient info is injected
- [ ] Test SIGNED status protection

### Code Quality
- [ ] Run static analysis: `mvn sonar:sonar`
- [ ] Check code style: `mvn spotbugs:check`
- [ ] Review code coverage report
- [ ] No hardcoded values

---

## Database Migration

### Before Deployment
- [ ] Backup database
  ```bash
  pg_dump clinic_db > backup_$(date +%Y%m%d_%H%M%S).sql
  ```
- [ ] Review migration SQL:
  ```bash
  cat src/main/resources/db/migration/V1.1__Add_medical_form_id_to_exam_view.sql
  ```
- [ ] Test migration on development database
- [ ] Verify medical_form_id column was added:
  ```sql
  SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS 
  WHERE TABLE_NAME='exam_view' AND COLUMN_NAME='medical_form_id';
  ```
- [ ] Verify index was created:
  ```sql
  SELECT * FROM pg_indexes WHERE tablename='exam_view' AND indexname='idx_exam_view_medical_form_id';
  ```

### During Deployment
- [ ] Confirm database is accessible
- [ ] Confirm Flyway migration runs successfully
- [ ] Check application logs for migration warnings
- [ ] Verify no rollback occurred

### After Deployment
- [ ] Query to count NULL medical_form_id:
  ```sql
  SELECT COUNT(*) as null_count FROM exam_view WHERE medical_form_id IS NULL;
  ```
- [ ] Verify index usage:
  ```sql
  EXPLAIN ANALYZE 
  SELECT * FROM exam_view WHERE medical_form_id = 'FORM-001';
  ```

---

## API Endpoint Testing

### Search Examinations (LAZY Load)
```bash
# Test 1: Basic search
curl -X GET "http://localhost:8080/examination?page=1"
# Expected: 200 OK, no results field

# Test 2: Search with keyword
curl -X GET "http://localhost:8080/examination?keyword=John&page=1"
# Expected: 200 OK, filtered results

# Test 3: Pagination
curl -X GET "http://localhost:8080/examination?page=2"
# Expected: 200 OK, page 2 results

# Verify response contains medicalFormId
# Expected: { "id": "...", "medicalFormId": "FORM-001", ... }
```

### Get Examination Details (EAGER Load + Filter)
```bash
# Test 1: Get valid exam
curl -X GET "http://localhost:8080/examination/EXAM-001"
# Expected: 200 OK, with results array

# Test 2: Verify REMOVED results filtered
# Expected: No results with status=REMOVED in response

# Test 3: Verify results count
curl -X GET "http://localhost:8080/examination/EXAM-001" | jq '.results | length'
# Expected: number of non-REMOVED results

# Test 4: Non-existent exam
curl -X GET "http://localhost:8080/examination/EXAM-999"
# Expected: 404 or empty response

# Verify response structure:
# {
#   "id": "...",
#   "patientId": "...",
#   "patientName": "...",
#   "patientEmail": "...",
#   "medicalFormId": "...",
#   "results": [
#     { "doctorId": "...", "status": "SIGNED", ... },
#     ...
#   ]
# }
```

### Update Result Status Protection
```bash
# Test 1: Update CREATED result (should succeed)
curl -X POST "http://localhost:8080/examination/EXAM-001/update-result" \
  -H "Content-Type: application/json" \
  -d '{"serviceId": "SERVICE-001", "newStatus": "SIGNED"}'
# Expected: 200 OK, event published

# Test 2: Update SIGNED result (should be ignored)
curl -X POST "http://localhost:8080/examination/EXAM-001/update-result" \
  -H "Content-Type: application/json" \
  -d '{"serviceId": "SERVICE-001", "newStatus": "CREATED"}'
# Expected: 200 OK, but no event published (silently ignored)

# Verify by querying again - status should still be SIGNED
curl -X GET "http://localhost:8080/examination/EXAM-001" | jq '.results[] | select(.serviceId=="SERVICE-001") | .status'
# Expected: "SIGNED"
```

---

## Performance Testing

### Load Testing
```bash
# Install Apache Bench
sudo apt-get install apache2-utils

# Test 1: Search endpoint (1000 requests, 10 concurrent)
ab -n 1000 -c 10 http://localhost:8080/examination?page=1
# Expected: Median response time <100ms

# Test 2: Details endpoint (1000 requests, 10 concurrent)
ab -n 1000 -c 10 http://localhost:8080/examination/EXAM-001
# Expected: Median response time <50ms

# Test 3: Under load (100 concurrent, 10000 requests)
ab -n 10000 -c 100 http://localhost:8080/examination?page=1
# Expected: >95% success rate, <200ms response time
```

### Query Performance
```sql
-- Test 1: Search query without results load
EXPLAIN ANALYZE 
SELECT * FROM exam_view WHERE deleted = false LIMIT 10;
-- Expected: <5ms, no sequential scan

-- Test 2: Details query with eager load
EXPLAIN ANALYZE 
SELECT DISTINCT e FROM ExamView e LEFT JOIN FETCH e.results WHERE e.id = 'EXAM-001';
-- Expected: <10ms, single query plan

-- Test 3: Index usage verification
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
WHERE tablename = 'exam_view';
-- Expected: idx_exam_view_medical_form_id used when filtering by medicalFormId
```

---

## Monitoring & Logging

### Log Levels
```properties
# Set log level to DEBUG for troubleshooting
logging.level.com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.projector=DEBUG
logging.level.com.clinic.c46.ExaminationService.application.handler.query=DEBUG
```

### Key Logs to Monitor
```
# Exam creation
[examination.projection.ExaminationCreatedEvent] EXAM-001

# Patient not found (WARNING)
[examination.projection.ExaminationCreatedEvent.patient-not-found] patientId: PATIENT-001

# SIGNED status protection (WARN)
[examination.update-result.command] Result with status SIGNED cannot be updated

# Result updates
[examination.projection.ResultStatusUpdatedEvent] SERVICE-001

# Result filtering
[examination.mapper.toExamDetailsDto] Filtering REMOVED results: 2 excluded
```

### Alerting
- [ ] Alert on "patient-not-found" errors
- [ ] Alert on query response time >500ms
- [ ] Alert on database migration failure
- [ ] Alert on SIGNED update attempts (security)

---

## Rollback Procedure

### If Issues Discovered

**Option 1: Quick Rollback (keep database)**
```bash
# 1. Stop current application
systemctl stop examination-service

# 2. Deploy previous version
java -jar ExaminationService-0.0.0-SNAPSHOT.jar

# 3. Verify working
curl http://localhost:8080/examination?page=1
```

**Option 2: Full Rollback (with database)**
```bash
# 1. Stop application
systemctl stop examination-service

# 2. Restore database
psql clinic_db < backup_20251120_120000.sql

# 3. Deploy previous version
java -jar ExaminationService-0.0.0-SNAPSHOT.jar

# 4. Verify
curl http://localhost:8080/examination?page=1
```

### Testing Rollback
- [ ] Test rollback on staging
- [ ] Verify all endpoints work after rollback
- [ ] Verify data integrity
- [ ] Verify no data loss

---

## Post-Deployment

### Day 1 Verification
- [ ] All endpoints responding
- [ ] No errors in application logs
- [ ] Database migration ran successfully
- [ ] Patient info being injected (non-null name/email)
- [ ] SIGNED status protection working
- [ ] REMOVED results being filtered
- [ ] Performance metrics within SLA

### Week 1 Monitoring
- [ ] Monitor error rates (target: <0.1%)
- [ ] Monitor response times (target: <200ms)
- [ ] Monitor database query counts (target: single query per request)
- [ ] Monitor SIGNED update blocks (should increase slightly)
- [ ] Monitor patient-not-found warnings (should be minimal)

### Week 4 Review
- [ ] Collect performance baseline
- [ ] Review and optimize slow queries
- [ ] Analyze usage patterns
- [ ] Get user feedback
- [ ] Plan next iteration

---

## Documentation

### For Developers
- [ ] Read `IMPLEMENTATION_GUIDE.md`
- [ ] Review `QUICK_REFERENCE.md`
- [ ] Check example code in test files
- [ ] Understand SIGNED status protection

### For Operations
- [ ] Read deployment instructions
- [ ] Understand database migration
- [ ] Know rollback procedures
- [ ] Monitor critical logs

### For Product/Business
- [ ] Understand new medicalFormId tracking
- [ ] Know SIGNED result immutability
- [ ] Review API changes
- [ ] Communicate to end users if needed

---

## Sign-Off

### Development Team
- [ ] Code reviewed by: _________________ Date: _______
- [ ] Tests verified by: ________________ Date: _______
- [ ] Code merged to main by: __________ Date: _______

### QA Team
- [ ] Testing completed by: _____________ Date: _______
- [ ] Performance testing by: __________ Date: _______
- [ ] Security testing by: _____________ Date: _______

### Operations Team
- [ ] Database backup verified: _________ Date: _______
- [ ] Migration tested: ________________ Date: _______
- [ ] Monitoring configured: __________ Date: _______
- [ ] Rollback plan tested: ___________ Date: _______

### Release Manager
- [ ] All checklists signed off: ________ Date: _______
- [ ] Approved for deployment: ________ Date: _______
- [ ] Deployed to production: ________ Date: _______

---

## Quick Reference Commands

```bash
# Build
cd ExaminationService && mvn clean package

# Test
mvn test

# Database migration
psql -U postgres -d clinic_db -f V1.1__Add_medical_form_id_to_exam_view.sql

# Deploy
java -jar target/ExaminationService-0.0.1-SNAPSHOT.jar

# Test endpoints
curl http://localhost:8080/examination?page=1
curl http://localhost:8080/examination/EXAM-001

# View logs
tail -f logs/examination-service.log | grep -E "ERROR|WARN|patient-not-found"

# Database health check
psql -c "SELECT COUNT(*) FROM exam_view WHERE medical_form_id IS NOT NULL;"
```

---

**Last Updated**: November 20, 2025  
**Version**: 1.1  
**Status**: Ready for Deployment

