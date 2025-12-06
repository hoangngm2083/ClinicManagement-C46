# Hướng Dẫn Fix Vector Store và Clinic API Connections

## 🔍 Vấn Đề Hiện Tại

Theo TEST_REPORT.md, có 2 vấn đề cần fix:

1. **Vector Store Connection**: `false` - pgvector extension chưa được cài
2. **Clinic API Connection**: `false` - Cần kiểm tra kết nối

## 🔧 Giải Pháp

### 1. Fix Vector Store (pgvector)

**Vấn đề**: PostgreSQL image `postgres:15-alpine` không có sẵn pgvector extension.

**Giải pháp**: Sử dụng image `pgvector/pgvector:pg15` thay vì `postgres:15-alpine`

#### Cách 1: Sửa docker-compose.yml (Khuyến nghị)

```yaml
postgres:
  image: pgvector/pgvector:pg15  # Thay đổi từ postgres:15-alpine
  container_name: postgres
  # ... rest of config
```

Sau đó:
```bash
docker-compose down postgres
docker-compose up -d postgres
```

#### Cách 2: Build custom image

Đã tạo Dockerfile tại `docker/postgres/Dockerfile`:
```dockerfile
FROM pgvector/pgvector:pg15
```

Build và sử dụng:
```bash
docker build -t clinic-postgres:pg15 docker/postgres/
```

Sửa docker-compose.yml:
```yaml
postgres:
  build: ./docker/postgres
  # hoặc
  image: clinic-postgres:pg15
```

#### Cách 3: Cài extension trong container (Tạm thời)

```bash
# Vào container
docker exec -it postgres sh

# Cài pgvector (nếu có package)
apk add postgresql15-pgvector

# Hoặc compile từ source (phức tạp hơn)
```

### 2. Fix Clinic API Connection

**Vấn đề**: Có thể do network hoặc timeout.

**Kiểm tra**:
```bash
# Test từ ai-service container
docker exec ai-service curl -s http://api-gateway:8080/api/staff?page=1

# Test từ host
curl -s http://localhost:8080/api/staff?page=1
```

**Fix**:
1. Đảm bảo API Gateway đang chạy:
   ```bash
   docker-compose ps api-gateway
   ```

2. Kiểm tra network:
   ```bash
   docker network inspect clinic-management-c46_c46-net
   ```

3. Kiểm tra timeout settings trong `AIService/app/services/clinic_api.py`

## 🧪 Test Sau Khi Fix

### Chạy script diagnostics:
```bash
cd AIService
source venv/bin/activate
python scripts/fix_connections.py
```

### Hoặc test từ container:
```bash
docker exec ai-service python3 -c "
from app.rag.pgvector_store import PGVectorStore
from app.services.clinic_api import ClinicAPIService
import asyncio

async def test():
    vs = PGVectorStore()
    print('Vector Store:', vs.health_check())
    
    async with ClinicAPIService() as api:
        doctors = await api.get_doctors(page=1)
        print('Clinic API:', len(doctors) > 0)

asyncio.run(test())
"
```

### Test health endpoint:
```bash
curl http://localhost:8000/health | python3 -m json.tool
```

Kết quả mong đợi:
```json
{
    "status": "healthy",
    "version": "1.0.0",
    "services": {
        "vector_store": true,
        "clinic_api": true,
        "agent": true
    }
}
```

## 📝 Các Bước Thực Hiện

### Bước 1: Backup (nếu cần)
```bash
docker exec postgres pg_dump -U booking vector_db > backup.sql
```

### Bước 2: Sửa docker-compose.yml
Thay đổi image postgres từ `postgres:15-alpine` sang `pgvector/pgvector:pg15`

### Bước 3: Restart services
```bash
docker-compose down postgres
docker-compose up -d postgres

# Đợi postgres khởi động
sleep 10

# Cài extension (nếu cần)
docker exec postgres psql -U booking -d vector_db -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

### Bước 4: Restart AI Service
```bash
docker-compose restart ai-service
```

### Bước 5: Verify
```bash
# Check health
curl http://localhost:8000/health

# Run diagnostics
cd AIService && source venv/bin/activate && python scripts/fix_connections.py
```

## ⚠️ Lưu Ý

1. **Data Loss**: Khi thay đổi image PostgreSQL, data có thể bị mất nếu không backup
2. **Migration**: Nếu đã có data, cần migrate sang image mới
3. **Network**: Đảm bảo các services trong cùng Docker network

## 🔗 Tài Liệu Tham Khảo

- pgvector Docker images: https://github.com/pgvector/pgvector
- pgvector documentation: https://github.com/pgvector/pgvector
- PostgreSQL extensions: https://www.postgresql.org/docs/current/contrib.html

## ✅ Checklist

- [ ] Backup database (nếu cần)
- [ ] Sửa docker-compose.yml
- [ ] Restart postgres service
- [ ] Cài pgvector extension
- [ ] Restart ai-service
- [ ] Test vector store connection
- [ ] Test clinic API connection
- [ ] Verify health endpoint
- [ ] Chạy lại tests

