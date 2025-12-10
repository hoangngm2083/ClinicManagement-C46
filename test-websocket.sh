#!/bin/bash

# WebSocket Connection Test Script
# Test WebSocket connectivity through the full stack: Nginx -> API Gateway -> Examination Flow Service

echo "🔍 Testing WebSocket connectivity..."
echo "=================================="

BASE_URL="http://localhost"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to test HTTP endpoint
test_http() {
    local url=$1
    local name=$2

    echo -n "Testing $name ($url): "
    if curl -s --max-time 5 "$url" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ OK${NC}"
        return 0
    else
        echo -e "${RED}❌ FAILED${NC}"
        return 1
    fi
}

# Function to test WebSocket endpoint (SockJS info)
test_websocket_info() {
    local url=$1
    local name=$2

    echo -n "Testing WebSocket $name ($url): "
    if curl -s --max-time 5 "$url" | grep -q "entropy\|serverId\|capabilities"; then
        echo -e "${GREEN}✅ OK${NC}"
        return 0
    else
        echo -e "${RED}❌ FAILED${NC}"
        return 1
    fi
}

echo "1. Testing service health endpoints..."
echo "--------------------------------------"

# Test individual services
test_http "http://localhost:9093/actuator/health" "Examination Flow Service Health"
test_http "http://localhost:8080/actuator/health" "API Gateway Health"

echo ""
echo "2. Testing WebSocket endpoints..."
echo "---------------------------------"

# Test WebSocket info endpoints
test_websocket_info "http://localhost:9093/ws/exam-workflow/info" "Direct Examination Flow WS"
test_websocket_info "http://localhost:8080/ws/exam-workflow/info" "Via API Gateway WS"
test_websocket_info "http://localhost/ws/exam-workflow/info" "Via Nginx WS"

echo ""
echo "3. Testing full WebSocket handshake..."
echo "---------------------------------------"

# Test WebSocket upgrade handshake
echo -n "Testing WebSocket handshake via Nginx: "
if curl -s -I -H "Upgrade: websocket" -H "Connection: Upgrade" \
       -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" \
       -H "Sec-WebSocket-Version: 13" \
       "http://localhost/ws/exam-workflow/123/abcdefgh/websocket" 2>/dev/null | \
       grep -q "101 Switching Protocols"; then
    echo -e "${GREEN}✅ OK${NC}"
else
    echo -e "${YELLOW}⚠️  May be OK (SockJS fallback)${NC}"
fi

echo ""
echo "4. Service status summary..."
echo "----------------------------"

# Check if services are running
echo "Docker containers status:"
docker-compose -f docker-compose.dev.yml ps --services --filter "status=running" | while read service; do
    if [ ! -z "$service" ]; then
        echo -e "  ${GREEN}✅${NC} $service"
    fi
done

echo ""
echo "📋 Next steps:"
echo "  - If WebSocket tests fail, check service logs:"
echo "    docker-compose -f docker-compose.dev.yml logs examination-flow-service"
echo "    docker-compose -f docker-compose.dev.yml logs api-gateway"
echo "    docker-compose -f docker-compose.dev.yml logs nginx"
echo ""
echo "  - Test with browser: Open dev tools → Network → WS tab"
echo "    Connect to: ws://localhost/ws/exam-workflow/123/abcdefgh/websocket"
echo ""
echo "  - For production, use: ws://your-domain.com/ws/exam-workflow/..."
echo ""

echo "🎉 WebSocket testing complete!"
