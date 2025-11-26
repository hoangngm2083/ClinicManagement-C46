# Hướng dẫn áp dụng các thay đổi

## Tổng quan
Đã khắc phục vấn đề "too many client connections" của PostgreSQL bằng cách:
1. Cấu hình đúng max_connections cho PostgreSQL
2. Thêm HikariCP connection pool configuration cho tất cả services

## Các thay đổi

### 1. docker-compose.yml
- ✅ Thêm command arguments để set max_connections=300
- ✅ Tối ưu hóa PostgreSQL memory settings

### 2. Application Properties (tất cả services)
- ✅ BookingService
- ✅ AuthService  
- ✅ PatientService
- ✅ StaffService
- ✅ MedicalPackageService
- ✅ ExaminationService
- ✅ ExaminationFlowService
- ✅ PaymentService
- ✅ NotificationService
- ✅ CommonService (application-common.properties)

## Cách áp dụng

### Bước 1: Rebuild services
```bash
# Dừng tất cả services
docker-compose down

# Rebuild tất cả services (bắt buộc để áp dụng cấu hình mới)
docker-compose build

# Hoặc rebuild từng service cụ thể
docker-compose build booking-service
docker-compose build auth-service
# ... các services khác
```

### Bước 2: Khởi động lại hệ thống
```bash
# Khởi động tất cả services
docker-compose up -d

# Hoặc khởi động và xem logs
docker-compose up
```

### Bước 3: Kiểm tra
```bash
# Kiểm tra tất cả services đã chạy
docker-compose ps

# Kiểm tra logs của PostgreSQL
docker-compose logs postgres

# Kiểm tra connections status
./check-db-connections.sh
```

## Kiểm tra chi tiết

### Kiểm tra PostgreSQL max_connections
```bash
docker exec postgres psql -U booking -d booking_db -c "SHOW max_connections;"
```
Kết quả mong đợi: **300**

### Kiểm tra số connections hiện tại
```bash
docker exec postgres psql -U booking -d booking_db -c "SELECT count(*) FROM pg_stat_activity;"
```

### Kiểm tra HikariCP metrics (cho mỗi service)
```bash
# Ví dụ với BookingService
curl http://localhost:8082/actuator/metrics/hikaricp.connections.active
curl http://localhost:8082/actuator/metrics/hikaricp.connections.idle
curl http://localhost:8082/actuator/metrics/hikaricp.connections
```

## Monitoring

### Sử dụng script kiểm tra
```bash
./check-db-connections.sh
```

Script này sẽ hiển thị:
- ✅ Max connections setting
- ✅ Total connections hiện tại
- ✅ Connections theo database
- ✅ Connections theo application
- ✅ Connection states
- ✅ Usage percentage với warnings

### Xem logs realtime
```bash
# Xem logs của tất cả services
docker-compose logs -f

# Xem logs của PostgreSQL
docker-compose logs -f postgres

# Xem logs của một service cụ thể
docker-compose logs -f booking-service
```

## Troubleshooting

### Vấn đề: Services không khởi động được
**Giải pháp:**
```bash
# Kiểm tra logs
docker-compose logs <service-name>

# Rebuild service
docker-compose build <service-name>

# Restart service
docker-compose restart <service-name>
```

### Vấn đề: Vẫn gặp "too many connections"
**Kiểm tra:**
1. Đảm bảo đã rebuild tất cả services
2. Kiểm tra PostgreSQL max_connections: `docker exec postgres psql -U booking -d booking_db -c "SHOW max_connections;"`
3. Xem connections hiện tại: `./check-db-connections.sh`
4. Kiểm tra logs để tìm connection leaks

**Giải pháp:**
- Giảm `maximum-pool-size` trong application.properties
- Kiểm tra code để đảm bảo connections được đóng đúng cách
- Xem xét sử dụng PgBouncer

### Vấn đề: Connection timeout
**Giải pháp:**
- Tăng `connection-timeout` trong HikariCP config
- Tăng `maximum-pool-size` nếu service cần nhiều connections
- Kiểm tra network latency giữa service và database

## Cấu hình HikariCP đã áp dụng

```properties
spring.datasource.hikari.maximum-pool-size=20      # Max 20 connections/service
spring.datasource.hikari.minimum-idle=5            # Min 5 idle connections
spring.datasource.hikari.connection-timeout=30000  # 30s timeout
spring.datasource.hikari.idle-timeout=600000       # 10 min idle timeout
spring.datasource.hikari.max-lifetime=1800000      # 30 min max lifetime
spring.datasource.hikari.pool-name=<service>-pool  # Pool name
```

## Tài liệu tham khảo

- 📄 Chi tiết kỹ thuật: [docs/DATABASE_CONNECTION_POOL.md](docs/DATABASE_CONNECTION_POOL.md)
- 🔧 Script kiểm tra: [check-db-connections.sh](check-db-connections.sh)

## Lưu ý quan trọng

⚠️ **Phải rebuild services** sau khi thay đổi application.properties
⚠️ **Monitoring connections** trong vài ngày đầu
⚠️ **Điều chỉnh pool size** nếu cần thiết dựa trên usage thực tế

## Hỗ trợ

Nếu gặp vấn đề, hãy:
1. Chạy `./check-db-connections.sh` để xem trạng thái
2. Kiểm tra logs: `docker-compose logs -f`
3. Xem metrics qua Actuator endpoints
