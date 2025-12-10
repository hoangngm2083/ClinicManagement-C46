# Docker Compose Setup Guide

Hệ thống Clinic Management sử dụng Docker Compose với 3 cấu hình khác nhau cho các môi trường khác nhau.

## Nginx: Host vs Container

### Tại sao dùng Nginx trong Docker Container?

**Ưu điểm:**
- **Consistency**: Cùng config across tất cả environments
- **Version Control**: Nginx config được track trong Git
- **Isolation**: Không conflict với system nginx
- **Easy Rollback**: Rollback cùng với app code
- **Simplified Deploy**: Một lệnh `docker-compose up` cho tất cả

**Performance Impact:**
- Overhead rất nhỏ (~1-2% CPU, ~10-20MB RAM)
- Với connection keepalive và caching, hiệu năng tương đương host installation

**Khi nào dùng Nginx trên Host?**
- High-traffic websites (>100k req/min)
- Complex SSL termination với hardware acceleration
- Integration với system monitoring tools (syslog, systemd)
- Khi đã có infrastructure team maintain nginx riêng

## File Docker Compose

### 1. `docker-compose.yml` (Legacy)
- File gốc ban đầu
- **Không khuyến nghị sử dụng**
- Chỉ để tham khảo hoặc tương thích ngược

### 2. `docker-compose.dev.yml` (Development)
- Môi trường phát triển
- Có debug ports cho remote debugging Java services
- Expose tất cả ports ra localhost
- Tối ưu cho development workflow

### 3. `docker-compose.deploy.yml` (Production)
- Môi trường production cho AWS EC2
- Tối ưu performance và bảo mật
- Sử dụng nginx reverse proxy
- Environment variables thay vì hardcode
- Resource limits và monitoring

## Cách sử dụng

### 🚀 Deploy siêu nhanh với script

```bash
# Development environment
./deploy.sh dev

# Production environment (default)
./deploy.sh prod
```

### Manual Deploy (nếu cần)

#### Development Environment

```bash
# Khởi động tất cả services cho development
docker-compose -f docker-compose.dev.yml up -d

# Hoặc sử dụng file gốc (không khuyến nghị)
docker-compose up -d

# Xem logs
docker-compose -f docker-compose.dev.yml logs -f

# Dừng services
docker-compose -f docker-compose.dev.yml down

# Dừng và xóa volumes
docker-compose -f docker-compose.dev.yml down -v
```

#### Debug Ports (Development)
- API Gateway: 8080
- Auth Service: 8081 (debug: 5006)
- Booking Service: 8082 (debug: 5005)
- Notification Service: 8083
- Patient Service: 8088 (debug: 5007)
- Staff Service: 8090 (debug: 5008)
- Medical Package Service: 8086 (debug: 5009)
- Examination Service: 9094 (debug: 5010)
- Examination Flow Service: 9093 (debug: 5011)
- Payment Service: 9098 (debug: 5012)
- AI Service: 8000
- HTML Server: 9999
- Axon Server Dashboard: 8024

### Production Environment (AWS EC2)

#### 🚀 Quick Deploy (Khuyến nghị)

```bash
# 1. Copy files to EC2
scp -i your-key.pem deploy.sh docker-compose.deploy.yml env.production nginx/ ec2-user@your-ec2:~/

# 2. SSH vào EC2 và setup
ssh -i your-key.pem ec2-user@your-ec2
mv env.production .env.prod
nano .env.prod  # Điền thông tin thực tế

# 3. Deploy với script
chmod +x deploy.sh
./deploy.sh prod
```

#### Manual Setup (nếu cần)

File `env.production` đã được tạo sẵn với dữ liệu từ project của bạn:

```bash
# File đã có sẵn với dữ liệu thực tế
# Chỉ cần update các thông tin sau:
nano env.production
```

Các thông tin cần điền:

```bash
# Database
DB_USER=booking
DB_PASSWORD=your_secure_password
DB_NAME=booking

# Email
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password

# Payment (VNPay)
VNP_TMN_CODE=your_vnpay_code
VNP_SECRET_KEY=your_vnpay_secret

# Axon Server
AXON_ACCESS_TOKEN=your_axon_token

# Public URL
PUBLIC_BASE_URL=https://your-domain.com
```

#### 2. Nginx Configuration (Docker Container)

Docker compose đã bao gồm nginx container. File config đã được tạo sẵn tại `nginx/nginx.conf` với:

- **Load balancing** cho API Gateway
- **Rate limiting** (10 req/s cho API, 5 req/s cho auth)
- **Gzip compression** để tối ưu bandwidth
- **Security headers** (XSS, CSRF protection)
- **Health check endpoint** tại `/health`
- **Performance tuning** (keepalive, connection pooling)

File config nginx đã được tối ưu cho production với:
- Connection keepalive để giảm latency
- Rate limiting để chống DDoS
- Gzip compression để giảm bandwidth
- Security headers để bảo mật
- Static file caching
- **WebSocket proxy support** cho real-time features

#### SSL Setup (Tùy chọn)

#### WebSocket Support

Hệ thống được cấu hình để hỗ trợ **WebSocket connections** cho real-time features:

- **Examination Flow Service** sử dụng WebSocket cho real-time queue updates
- **Endpoint**: `/ws/exam-workflow` (SockJS with STOMP protocol)
- **Architecture**: Client ↔ Nginx ↔ API Gateway ↔ Examination Flow Service
- **Timeout**: 7 days cho persistent connections

**WebSocket Flow:**
```
Client (Browser)
    ↓ (WebSocket/STOMP)
Nginx (proxy with Upgrade headers)
    ↓ (WebSocket route)
API Gateway (ws:// route)
    ↓ (WebSocket)
Examination Flow Service (/ws/exam-workflow)
```

**SSL Setup (khi cần):**

```bash
# 1. Cài certbot trên EC2
sudo yum install certbot -y  # Amazon Linux
sudo certbot certonly --standalone -d your-domain.com

# 2. Copy certificates vào thư mục nginx
sudo cp /etc/letsencrypt/live/your-domain.com/fullchain.pem ./nginx/ssl/cert.pem
sudo cp /etc/letsencrypt/live/your-domain.com/privkey.pem ./nginx/ssl/key.pem
sudo chown 1000:1000 ./nginx/ssl/*.pem

# 3. Uncomment SSL server block trong nginx/nginx.conf
# 4. Restart nginx
docker-compose -f docker-compose.deploy.yml restart nginx

# 5. Setup auto-renew (optional)
echo "0 12 * * * /usr/bin/certbot renew --quiet && docker-compose -f docker-compose.deploy.yml restart nginx" | sudo crontab -
```

**Lưu ý:** SSL sẽ được setup sau khi hệ thống chạy ổn định.

#### 3. Deploy lên AWS EC2

```bash
# Copy files to EC2
scp -i your-key.pem docker-compose.deploy.yml env.production nginx/ ec2-user@your-ec2-instance:~

# SSH vào EC2
ssh -i your-key.pem ec2-user@your-ec2-instance

# Đổi tên file env
mv env.production .env.prod

# Khởi động services
docker-compose -f docker-compose.deploy.yml --env-file .env.prod up -d

# Kiểm tra status
docker-compose -f docker-compose.deploy.yml ps

# Kiểm tra health
curl http://localhost/health

# Xem logs
docker-compose -f docker-compose.deploy.yml logs -f

# Xem logs từng service
docker-compose -f docker-compose.deploy.yml logs -f nginx
```

## Services Overview

### Database Services
- **PostgreSQL**: Cơ sở dữ liệu chính với pgvector extension
- **Redis**: Cache và session storage
- **Axon Server**: Event sourcing và CQRS

### Microservices
- **API Gateway**: Điểm entry chính của hệ thống
- **Auth Service**: Xác thực và phân quyền
- **Booking Service**: Quản lý đặt lịch
- **Patient Service**: Thông tin bệnh nhân
- **Staff Service**: Quản lý nhân viên
- **Medical Package Service**: Gói khám
- **Examination Service**: Quản lý khám bệnh
- **Examination Flow Service**: Quy trình khám
- **Payment Service**: Thanh toán VNPay
- **Notification Service**: Gửi email thông báo
- **AI Service**: AI assistant cho hệ thống

### Utilities
- **Python Server**: Serve static HTML files
- **Nginx**: Reverse proxy và load balancer (production only)

## Monitoring và Health Checks

Tất cả services đều có health checks:
- **Development**: Interval 20-30s
- **Production**: Interval 30-60s với retry logic

```bash
# Kiểm tra health của tất cả services
docker-compose -f docker-compose.dev.yml ps

# Kiểm tra health của service cụ thể
docker-compose -f docker-compose.dev.yml exec api-gateway wget -qO- http://localhost:8080/actuator/health
```

## Troubleshooting

### Common Issues

1. **Port conflicts**: Đảm bảo ports không bị chiếm bởi services khác
2. **Memory issues**: Tăng RAM EC2 instance hoặc giảm resource limits
3. **Database connection**: Kiểm tra environment variables và network connectivity
4. **SSL certificates**: Đảm bảo cert.pem và key.pem được copy đúng vào nginx/ssl/

### Logs và Debugging

```bash
# Xem logs tất cả services
docker-compose -f docker-compose.dev.yml logs -f

# Xem logs service cụ thể
docker-compose -f docker-compose.dev.yml logs -f api-gateway

# Vào container để debug
docker-compose -f docker-compose.dev.yml exec api-gateway bash

# Kiểm tra resource usage
docker stats
```

### Nginx Container Management

```bash
# Kiểm tra nginx config
docker-compose -f docker-compose.deploy.yml exec nginx nginx -t

# Reload nginx config mà không restart
docker-compose -f docker-compose.deploy.yml exec nginx nginx -s reload

# Xem nginx access logs
docker-compose -f docker-compose.deploy.yml exec nginx tail -f /var/log/nginx/access.log

# Test upstream connectivity
docker-compose -f docker-compose.deploy.yml exec nginx wget -qO- http://api-gateway:8080/actuator/health

# Test WebSocket connectivity
docker-compose -f docker-compose.deploy.yml exec nginx wget -qO- http://api-gateway:8080/ws/exam-workflow/info

# Monitor nginx performance
docker-compose -f docker-compose.deploy.yml exec nginx nginx -V  # Version info
```

### WebSocket Troubleshooting

```bash
# Kiểm tra WebSocket endpoint trực tiếp
curl -I http://localhost:9093/ws/exam-workflow

# Kiểm tra qua API Gateway
curl -I http://localhost:8080/ws/exam-workflow

# Kiểm tra qua Nginx
curl -I http://localhost/ws/exam-workflow

# Test đầy đủ với script tự động
./test-websocket.sh

# Xem WebSocket handshake logs
docker-compose -f docker-compose.deploy.yml logs nginx | grep "ws/exam-workflow"

# Test WebSocket connection với client
# Sử dụng browser dev tools hoặc WebSocket client test
```

### Backup và Restore

```bash
# Backup database
docker-compose -f docker-compose.deploy.yml exec postgres pg_dump -U booking booking_db > backup.sql

# Backup nginx logs (nếu cần)
docker-compose -f docker-compose.deploy.yml exec nginx tar czf /tmp/nginx-logs.tar.gz /var/log/nginx/
docker cp $(docker-compose -f docker-compose.deploy.yml ps -q nginx):/tmp/nginx-logs.tar.gz ./nginx-logs.tar.gz

# Restore database
docker-compose -f docker-compose.deploy.yml exec -T postgres psql -U booking booking_db < backup.sql
```

## Performance Tuning

### Development
- Memory: 512MB - 1GB per service
- CPU: Shared resources
- Health check: Frequent (20-30s)

### Production
- Memory: 512MB - 4GB per service (tùy theo service)
- CPU: Dedicated cores cho critical services
- Health check: Less frequent (30-60s)
- Database: Tối ưu với shared_buffers=1GB, work_mem=4MB

## Security Notes

- **Production**: Không expose internal ports ra internet
- **Environment Variables**: Luôn sử dụng .env files, không commit secrets
- **SSL**: Luôn enable HTTPS trong production
- **Firewall**: Chỉ mở ports 80, 443, 22 trên EC2
- **Updates**: Thường xuyên update Docker images để patch security
