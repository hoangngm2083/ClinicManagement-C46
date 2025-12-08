#!/bin/bash

# Full Test Suite Runner for AI Service
# Chạy tất cả tests và tạo báo cáo

set -e

echo "=================================================================================="
echo "                    CHẠY FULL TEST SUITE - AI SERVICE"
echo "=================================================================================="
echo ""

cd "$(dirname "$0")/.."

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Check if venv exists
if [ ! -d "venv" ]; then
    echo -e "${RED}❌ venv not found. Please create venv first.${NC}"
    exit 1
fi

# Activate venv
source venv/bin/activate

echo -e "${BLUE}📋 Test Plan:${NC}"
echo "  1. Health Tests"
echo "  2. Clinic Info Tests"
echo "  3. Booking Tests"
echo "  4. Memory Tests"
echo "  5. Integration Tests"
echo ""

# Run tests
echo -e "${BLUE}🚀 Running tests...${NC}"
echo ""

# Health tests
echo -e "${YELLOW}1. Health Tests${NC}"
pytest tests/test_health.py -v --tb=short
HEALTH_RESULT=$?

echo ""

# Clinic info tests (via agent only)
echo -e "${YELLOW}2. Clinic Info Tests (via agent)${NC}"
pytest tests/test_clinic_info.py::test_clinic_info_via_agent tests/test_clinic_info.py::test_clinic_info_multiple_queries_same_session -v --tb=short
CLINIC_RESULT=$?

echo ""

# Booking tests (via agent only)
echo -e "${YELLOW}3. Booking Tests (via agent)${NC}"
pytest tests/test_booking.py::test_full_booking_flow_via_agent -v --tb=short
BOOKING_RESULT=$?

echo ""

# Memory tests
echo -e "${YELLOW}4. Memory Tests${NC}"
pytest tests/test_memory.py -v --tb=short
MEMORY_RESULT=$?

echo ""

# Integration tests
echo -e "${YELLOW}5. Integration Tests${NC}"
pytest tests/test_integration.py -v --tb=short
INTEGRATION_RESULT=$?

echo ""
echo "=================================================================================="
echo -e "${BLUE}📊 Test Summary${NC}"
echo "=================================================================================="

# Calculate results
TOTAL_PASSED=0
TOTAL_FAILED=0

if [ $HEALTH_RESULT -eq 0 ]; then
    echo -e "${GREEN}✅ Health Tests: PASSED${NC}"
    TOTAL_PASSED=$((TOTAL_PASSED + 5))
else
    echo -e "${RED}❌ Health Tests: FAILED${NC}"
    TOTAL_FAILED=$((TOTAL_FAILED + 1))
fi

if [ $CLINIC_RESULT -eq 0 ]; then
    echo -e "${GREEN}✅ Clinic Info Tests: PASSED${NC}"
    TOTAL_PASSED=$((TOTAL_PASSED + 2))
else
    echo -e "${RED}❌ Clinic Info Tests: FAILED${NC}"
    TOTAL_FAILED=$((TOTAL_FAILED + 1))
fi

if [ $BOOKING_RESULT -eq 0 ]; then
    echo -e "${GREEN}✅ Booking Tests: PASSED${NC}"
    TOTAL_PASSED=$((TOTAL_PASSED + 1))
else
    echo -e "${RED}❌ Booking Tests: FAILED${NC}"
    TOTAL_FAILED=$((TOTAL_FAILED + 1))
fi

if [ $MEMORY_RESULT -eq 0 ]; then
    echo -e "${GREEN}✅ Memory Tests: PASSED${NC}"
    TOTAL_PASSED=$((TOTAL_PASSED + 11))
else
    echo -e "${RED}❌ Memory Tests: FAILED${NC}"
    TOTAL_FAILED=$((TOTAL_FAILED + 1))
fi

if [ $INTEGRATION_RESULT -eq 0 ]; then
    echo -e "${GREEN}✅ Integration Tests: PASSED${NC}"
    TOTAL_PASSED=$((TOTAL_PASSED + 12))
else
    echo -e "${RED}❌ Integration Tests: FAILED${NC}"
    TOTAL_FAILED=$((TOTAL_FAILED + 1))
fi

echo ""
echo -e "${BLUE}Tổng cộng: ${GREEN}${TOTAL_PASSED} passed${NC}, ${RED}${TOTAL_FAILED} failed${NC}"
echo ""

# Check Docker services
echo "=================================================================================="
echo -e "${BLUE}🐳 Docker Services Status${NC}"
echo "=================================================================================="
cd ../..
docker-compose ps | grep -E "(ai-service|api-gateway|postgres)" | head -3
echo ""

# API Health Check
echo "=================================================================================="
echo -e "${BLUE}🏥 API Health Check${NC}"
echo "=================================================================================="
curl -s http://localhost:8000/health | python3 -m json.tool 2>/dev/null || echo "Service not accessible"
echo ""

echo "=================================================================================="
if [ $TOTAL_FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 All critical tests passed!${NC}"
    exit 0
else
    echo -e "${YELLOW}⚠️  Some tests failed. Check details above.${NC}"
    exit 1
fi

