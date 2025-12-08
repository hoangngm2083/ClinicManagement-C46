# Kiểm tra và Sửa lỗi Dữ liệu Gói Khám

## Vấn đề

Dữ liệu gói khám trong system prompt (dòng 60 của `prompts.py`) có thể không chính xác vì:
1. API call thất bại và đang dùng `FALLBACK_PACKAGES_LIST` (hardcoded data)
2. API trả về danh sách rỗng
3. DataSyncService không được khởi động nên không có periodic sync

## Giải pháp đã thực hiện

### 1. Thêm DataSyncService vào main.py
- Khởi động DataSyncService khi app start
- Tự động sync dữ liệu định kỳ:
  - Doctors: mỗi 15 phút
  - Packages: mỗi 30 phút  
  - Slots: mỗi 5 phút
- Dừng service khi app shutdown

### 2. Cải thiện Logging
- Thêm logging chi tiết trong `build_dynamic_system_prompt()`
- Log khi nào dùng fallback data
- Log số lượng packages được load

### 3. Thêm Admin Endpoints
- `/admin/sync-status`: Kiểm tra trạng thái sync service
- `/admin/sync-now`: Trigger manual sync ngay lập tức
- `/admin/prompt-preview`: Xem system prompt hiện tại

## Cách kiểm tra

### 1. Chạy script kiểm tra
```bash
cd AIService
python3 scripts/check_medical_packages.py
```

Script này sẽ:
- Kiểm tra API có trả về dữ liệu không
- So sánh với fallback data
- Hiển thị phần gói khám trong system prompt

### 2. Kiểm tra logs khi khởi động
Tìm các log messages:
- `"Fetching medical packages from API..."`
- `"Successfully loaded X medical packages from API"` (nếu thành công)
- `"API returned empty packages list, using FALLBACK_PACKAGES_LIST"` (nếu rỗng)
- `"Failed to load medical packages from API"` (nếu lỗi)

### 3. Kiểm tra qua API endpoints

#### Xem system prompt hiện tại:
```bash
curl http://localhost:8000/admin/prompt-preview
```

#### Kiểm tra sync status:
```bash
curl http://localhost:8000/admin/sync-status
```

#### Trigger manual sync:
```bash
curl -X POST http://localhost:8000/admin/sync-now
```

### 4. Kiểm tra trong code

Trong `prompts.py`, hàm `build_dynamic_system_prompt()`:
- Gọi `clinic_api.get_medical_packages()` để lấy dữ liệu từ API
- Nếu API fail hoặc trả về rỗng → dùng `FALLBACK_PACKAGES_LIST`
- Log rõ ràng khi nào dùng fallback

## Các nguyên nhân có thể

### 1. API Gateway không chạy
- Kiểm tra: `http://api-gateway:8080/api/medical-package`
- Xem logs của API Gateway

### 2. MedicalPackageService không chạy
- Kiểm tra service có đang chạy không
- Kiểm tra logs của MedicalPackageService

### 3. Database không có dữ liệu
- Cần chạy migration để tạo test data
- Endpoint: `POST /migrate/database` trong MedicalPackageService

### 4. Network connectivity
- Kiểm tra kết nối giữa AIService và API Gateway
- Kiểm tra DNS resolution (`api-gateway`)

## Cách sửa

### Nếu API không trả về dữ liệu:

1. **Kiểm tra MedicalPackageService có dữ liệu không:**
   ```bash
   # Trong MedicalPackageService
   curl http://localhost:8080/medical-package
   ```

2. **Nếu không có dữ liệu, chạy migration:**
   ```bash
   # Trong MedicalPackageService
   curl -X POST http://localhost:8080/migrate/database
   ```

3. **Kiểm tra API Gateway routing:**
   - Xem config của API Gateway
   - Đảm bảo route `/api/medical-package` đúng

4. **Trigger manual sync sau khi có dữ liệu:**
   ```bash
   curl -X POST http://localhost:8000/admin/sync-now
   ```

### Nếu đang dùng fallback data:

1. Kiểm tra logs để xem lỗi cụ thể
2. Sửa lỗi kết nối/API
3. Clear cache và reload:
   ```bash
   curl -X POST http://localhost:8000/admin/clear-prompt-cache
   curl -X POST http://localhost:8000/admin/sync-now
   ```

## Monitoring

Sau khi sửa, monitor:
- Logs của AIService để xem sync có chạy không
- `/admin/sync-status` để xem lịch sync
- `/admin/prompt-preview` để xem dữ liệu hiện tại

## Lưu ý

- System prompt được cache 1 giờ (TTL)
- Nếu cần cập nhật ngay, gọi `/admin/clear-prompt-cache`
- DataSyncService tự động clear cache khi sync packages thành công

