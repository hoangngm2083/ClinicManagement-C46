# Khắc phục vấn đề "Too Many Client Connections" PostgreSQL

## Vấn đề
Hệ thống microservices gặp lỗi "too many client connections" khi nhiều services cùng kết nối đến PostgreSQL database.

## Nguyên nhân
1. **PostgreSQL max_connections không được cấu hình đúng**: Biến môi trường `POSTGRES_MAX_CONNECTIONS` không có tác dụng với PostgreSQL image
2. **Không có giới hạn connection pool**: Mỗi service sử dụng HikariCP với cấu hình mặc định (10 connections), với 8 services có thể tạo ra 80+ connections
3. **Connection leaks**: Connections không được đóng đúng cách hoặc bị giữ quá lâu

## Giải pháp đã áp dụng

### 1. Cấu hình PostgreSQL (docker-compose.yml)
- **Tăng max_connections lên 300** thông qua command line arguments
- **Tối ưu hóa memory settings**:
  - `shared_buffers=256MB`: Bộ nhớ cache cho PostgreSQL
  - `effective_cache_size=1GB`: Ước tính cache của OS
  - `work_mem=2MB`: Bộ nhớ cho mỗi operation
  - `maintenance_work_mem=64MB`: Bộ nhớ cho maintenance tasks

### 2. Cấu hình HikariCP Connection Pool cho tất cả services

Đã thêm cấu hình sau vào tất cả các services:

```properties
# HikariCP Connection Pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.pool-name=<service-name>-pool
```

**Giải thích các tham số:**
- `maximum-pool-size=20`: Tối đa 20 connections mỗi service
- `minimum-idle=5`: Giữ tối thiểu 5 idle connections
- `connection-timeout=30000`: Timeout 30s khi lấy connection
- `idle-timeout=600000`: Đóng idle connections sau 10 phút
- `max-lifetime=1800000`: Đóng connections sau 30 phút (tránh connection leaks)

### 3. Tính toán Connections

**Tổng số connections tối đa:**
- 8 services × 20 connections/service = **160 connections**
- PostgreSQL max_connections = **300**
- **Dư trữ: 140 connections** cho các connections khác (admin, monitoring, etc.)

## Services đã được cập nhật

1. ✅ BookingService
2. ✅ AuthService
3. ✅ PatientService
4. ✅ StaffService
5. ✅ MedicalPackageService
6. ✅ ExaminationService
7. ✅ ExaminationFlowService
8. ✅ PaymentService
9. ✅ NotificationService

## Cách kiểm tra

### 1. Kiểm tra số connections hiện tại trong PostgreSQL:
```sql
SELECT count(*) FROM pg_stat_activity;
```

### 2. Xem chi tiết connections theo database:
```sql
SELECT datname, count(*) 
FROM pg_stat_activity 
GROUP BY datname;
```

### 3. Xem connections theo application:
```sql
SELECT application_name, count(*) 
FROM pg_stat_activity 
WHERE application_name != '' 
GROUP BY application_name;
```

### 4. Kiểm tra max_connections setting:
```sql
SHOW max_connections;
```

## Monitoring

Để theo dõi connection pool, bạn có thể:

1. **Xem HikariCP metrics** qua Spring Boot Actuator:
   - Endpoint: `http://localhost:<port>/actuator/metrics/hikaricp.connections`
   - Metrics: active, idle, pending, total connections

2. **Logs**: HikariCP sẽ log warning nếu:
   - Connection timeout xảy ra
   - Connection leak được phát hiện (sau 60s)

## Khuyến nghị

1. **Rebuild và restart tất cả services** để áp dụng cấu hình mới:
   ```bash
   docker-compose down
   docker-compose build
   docker-compose up -d
   ```

2. **Monitor connections** trong vài ngày đầu để đảm bảo không còn vấn đề

3. **Điều chỉnh pool size** nếu cần:
   - Nếu thấy nhiều connection timeout → tăng `maximum-pool-size`
   - Nếu database vẫn quá tải → giảm `maximum-pool-size`

4. **Xem xét connection pooling ở database level** (PgBouncer) nếu vấn đề vẫn tiếp diễn

## Lưu ý

- Cấu hình HikariCP đã được thêm vào file `application-common.properties` trong CommonService
- Các services riêng lẻ cũng có cấu hình riêng để đảm bảo hoạt động độc lập
- Connection leak detection được bật với threshold 60s để phát hiện các connection không được đóng đúng cách
