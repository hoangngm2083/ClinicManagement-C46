#!/bin/sh
set -e

# Tên file config được truyền từ CMD/Docker Compose
REDIS_CONFIG="/usr/local/etc/redis/redis.conf"
PROC_KEY="queue:processing"
INIT_FLAG_KEY="${PROC_KEY}:init_flag"
RECEP_KEY="queue:reception"
RECEP_INIT_FLAG_KEY="${RECEP_KEY}:init_flag"

echo "Bắt đầu kiểm tra và khởi tạo Redis Queue..."

# --- 1. CHẠY REDIS TẠM THỜI Ở CHẾ ĐỘ NỀN (DAEMON) ---

# Sử dụng tên lệnh 'redis-server' trực tiếp (đã nằm trong $PATH)
# Chạy Redis server tạm thời, sử dụng file config đã mount và chạy nền
redis-server $REDIS_CONFIG --daemonize yes --pidfile /tmp/redis.pid

# Đợi 2 giây để server chắc chắn đã sẵn sàng kết nối
sleep 2 

# Kiểm tra PING để đảm bảo server tạm thời đã sẵn sàng
if ! redis-cli PING; then
    echo "Lỗi: Redis server tạm thời không khởi động được hoặc không phản hồi PING."
    # Dừng script để tránh lỗi nghiêm trọng
    exit 1
fi

# --- 2. THỰC HIỆN LOGIC KHỞI TẠO KEY (Atomic) ---

echo "Kiểm tra Key Flag: $INIT_FLAG_KEY"

# Sử dụng SETNX để đảm bảo chỉ có 1 tiến trình khởi tạo
# Lệnh 'redis-cli SETNX $INIT_FLAG_KEY 1' trả về 1 nếu thành công (khởi tạo)
if redis-cli SETNX $INIT_FLAG_KEY 1; then
    echo "=> Global Queue chưa tồn tại. Bắt đầu khởi tạo List rỗng: $PROC_KEY"
    
    # Logic tạo List rỗng vật lý:
    # 1. LPUSH phần tử placeholder (tạo Key List nếu chưa tồn tại)
    redis-cli LPUSH $PROC_KEY "__INIT_PLACEHOLDER__"
    # 2. LTRIM List về kích thước rỗng (đảm bảo List rỗng)
    redis-cli LTRIM $PROC_KEY 1 0 
    
    echo "=> Khởi tạo hoàn tất. Cờ $INIT_FLAG_KEY đã được đặt."
else
    echo "=> Global Queue đã được khởi tạo trước đó (hoặc đang được tiến trình khác xử lý). Bỏ qua."
fi

# Khởi tạo Queue Reception
echo "Kiểm tra Key Flag: $RECEP_INIT_FLAG_KEY"

# Sử dụng SETNX để đảm bảo chỉ có 1 tiến trình khởi tạo
if redis-cli SETNX $RECEP_INIT_FLAG_KEY 1; then
    echo "=> Reception Queue chưa tồn tại. Bắt đầu khởi tạo List rỗng: $RECEP_KEY"
    
    # Logic tạo List rỗng vật lý:
    # 1. LPUSH phần tử placeholder (tạo Key List nếu chưa tồn tại)
    redis-cli LPUSH $RECEP_KEY "__INIT_PLACEHOLDER__"
    # 2. LTRIM List về kích thước rỗng (đảm bảo List rỗng)
    redis-cli LTRIM $RECEP_KEY 1 0 
    
    echo "=> Khởi tạo hoàn tất. Cờ $RECEP_INIT_FLAG_KEY đã được đặt."
else
    echo "=> Reception Queue đã được khởi tạo trước đó (hoặc đang được tiến trình khác xử lý). Bỏ qua."
fi

# --- 3. DỪNG SERVER TẠM THỜI ---

echo "Dừng Redis server tạm thời..."
# Lệnh shutdown an toàn
redis-cli -p 6379 shutdown
sleep 1 # Đợi server dừng

# --- 4. CHẠY LỆNH REDIS CHÍNH THỨC (FOREGROUND) ---

# Dùng exec để thay thế tiến trình shell bằng tiến trình Redis Server chính
# "$@" là các tham số được truyền từ CMD (redis-server /usr/local/etc/redis/redis.conf)
echo "Khởi động Redis Server chính thức (Foreground)..."
exec "$@"