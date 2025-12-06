#!/bin/bash

# Integration test script for refactored AIService
# This script tests the full functionality with all microservices

set -e

echo "🚀 Starting AIService Integration Test"
echo "======================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print status
print_status() {
    local status=$1
    local message=$2
    if [ "$status" -eq 0 ]; then
        echo -e "${GREEN}✅ $message${NC}"
    else
        echo -e "${RED}❌ $message${NC}"
    fi
}

# Step 1: Start microservices
echo "📦 Step 1: Starting microservices..."
docker compose up -d postgres redis axon-server api-gateway booking-service staff-service medical-package-service
sleep 30  # Wait for services to be healthy

# Check if services are healthy
echo "🔍 Checking service health..."

# Check PostgreSQL
if docker compose exec -T postgres pg_isready -U booking > /dev/null 2>&1; then
    print_status 0 "PostgreSQL is healthy"
else
    print_status 1 "PostgreSQL is not healthy"
    exit 1
fi

# Check API Gateway
if curl -s http://localhost:8080/actuator/health > /dev/null; then
    print_status 0 "API Gateway is healthy"
else
    print_status 1 "API Gateway is not healthy"
    exit 1
fi

# Check Staff Service
if curl -s http://localhost:8090/actuator/health > /dev/null; then
    print_status 0 "Staff Service is healthy"
else
    print_status 1 "Staff Service is not healthy"
    exit 1
fi

# Check Medical Package Service
if curl -s http://localhost:8086/actuator/health > /dev/null; then
    print_status 0 "Medical Package Service is healthy"
else
    print_status 1 "Medical Package Service is not healthy"
    exit 1
fi

# Check Booking Service
if curl -s http://localhost:8082/actuator/health > /dev/null; then
    print_status 0 "Booking Service is healthy"
else
    print_status 1 "Booking Service is not healthy"
    exit 1
fi

echo

# Step 2: Start AI Service
echo "🤖 Step 2: Starting AI Service..."
cd AIService
docker compose up -d ai-service
sleep 15  # Wait for AI service to start

# Check AI Service health
if curl -s http://localhost:8000/health > /dev/null; then
    print_status 0 "AI Service is healthy"
else
    print_status 1 "AI Service is not healthy"
    exit 1
fi

echo

# Step 3: Test basic chat functionality
echo "💬 Step 3: Testing basic chat functionality..."

# Test 1: Basic greeting
response=$(curl -s -X POST http://localhost:8000/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Xin chào", "session_id": "test_001"}')

if echo "$response" | grep -q "response"; then
    print_status 0 "Basic chat test passed"
else
    print_status 1 "Basic chat test failed"
    echo "Response: $response"
fi

echo

# Step 4: Test doctor search functionality
echo "👨‍⚕️ Step 4: Testing doctor search functionality..."

response=$(curl -s -X POST http://localhost:8000/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Tôi muốn tìm bác sĩ răng miệng", "session_id": "test_002"}')

if echo "$response" | grep -q "Bác sĩ"; then
    print_status 0 "Doctor search test passed"
else
    print_status 1 "Doctor search test failed"
    echo "Response: $response"
fi

echo

# Step 5: Test package recommendation
echo "📦 Step 5: Testing package recommendation..."

response=$(curl -s -X POST http://localhost:8000/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Tôi bị đau răng, cần khám gì?", "session_id": "test_003"}')

if echo "$response" | grep -q "khám\|gói"; then
    print_status 0 "Package recommendation test passed"
else
    print_status 1 "Package recommendation test failed"
    echo "Response: $response"
fi

echo

# Step 6: Test clinic information query
echo "🏥 Step 6: Testing clinic information query..."

response=$(curl -s -X POST http://localhost:8000/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Phòng khám mở cửa mấy giờ?", "session_id": "test_004"}')

if echo "$response" | grep -q "giờ\|mở cửa"; then
    print_status 0 "Clinic info query test passed"
else
    print_status 1 "Clinic info query test failed"
    echo "Response: $response"
fi

echo

# Step 7: Test conversation memory
echo "🧠 Step 7: Testing conversation memory..."

# First message
curl -s -X POST http://localhost:8000/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Tôi muốn đặt lịch khám", "session_id": "memory_test"}' > /dev/null

# Follow-up message (should remember context)
response=$(curl -s -X POST http://localhost:8000/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Tôi bị đau răng số 6", "session_id": "memory_test"}')

if echo "$response" | grep -q "răng\|đặt lịch"; then
    print_status 0 "Conversation memory test passed"
else
    print_status 1 "Conversation memory test failed"
    echo "Response: $response"
fi

echo

# Step 8: Test admin endpoints
echo "⚙️ Step 8: Testing admin endpoints..."

# Test prompt preview
if curl -s http://localhost:8000/admin/prompt-preview > /dev/null; then
    print_status 0 "Admin prompt preview test passed"
else
    print_status 1 "Admin prompt preview test failed"
fi

# Test cache clearing
if curl -s -X POST http://localhost:8000/admin/clear-prompt-cache > /dev/null; then
    print_status 0 "Admin cache clearing test passed"
else
    print_status 1 "Admin cache clearing test failed"
fi

echo

# Step 9: Test error handling
echo "🚨 Step 9: Testing error handling..."

# Test with invalid data
response=$(curl -s -X POST http://localhost:8000/chat \
  -H "Content-Type: application/json" \
  -d '{"invalid": "data"}')

if echo "$response" | grep -q "error\|lỗi"; then
    print_status 0 "Error handling test passed"
else
    print_status 1 "Error handling test failed"
    echo "Response: $response"
fi

echo
echo "🎉 Integration testing completed!"
echo
echo "📊 Manual verification steps:"
echo "1. Check AI Service logs: docker compose logs -f ai-service"
echo "2. Test booking flow manually through API"
echo "3. Verify vector data is being indexed in PostgreSQL"
echo "4. Test concurrent sessions and memory persistence"

# Cleanup (optional - comment out if you want to keep services running)
echo
read -p "🧹 Do you want to stop all services? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Stopping services..."
    docker compose down
    print_status 0 "Services stopped successfully"
fi
