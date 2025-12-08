#!/bin/bash

# Script to run all AI Service tests
# Usage: ./run_all_tests.sh [options]

set -e

echo "🧪 Running AI Service Tests"
echo "============================"
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default options
PYTEST_OPTS="-v --tb=short"
COVERAGE=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --coverage)
            COVERAGE=true
            shift
            ;;
        --verbose|-v)
            PYTEST_OPTS="-vv --tb=long"
            shift
            ;;
        --quiet|-q)
            PYTEST_OPTS="-q"
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [options]"
            echo ""
            echo "Options:"
            echo "  --coverage    Run tests with coverage report"
            echo "  --verbose, -v Run tests in verbose mode"
            echo "  --quiet, -q   Run tests in quiet mode"
            echo "  --help, -h    Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Check if we're in the right directory
if [ ! -d "tests" ]; then
    echo "❌ Error: tests directory not found. Please run from AIService directory."
    exit 1
fi

# Check if pytest is installed
if ! command -v pytest &> /dev/null; then
    echo "❌ Error: pytest not found. Please install: pip install pytest pytest-asyncio"
    exit 1
fi

echo -e "${BLUE}📋 Test Plan:${NC}"
echo "  1. Health and basic endpoints"
echo "  2. Clinic information functionality"
echo "  3. Appointment booking functionality"
echo "  4. LangGraph memory functionality"
echo "  5. Full integration tests"
echo ""

# Run tests
if [ "$COVERAGE" = true ]; then
    echo -e "${BLUE}📊 Running tests with coverage...${NC}"
    pytest $PYTEST_OPTS \
        --cov=app \
        --cov-report=html \
        --cov-report=term \
        tests/
    
    echo ""
    echo -e "${GREEN}✅ Coverage report generated in htmlcov/index.html${NC}"
else
    echo -e "${BLUE}🚀 Running all tests...${NC}"
    pytest $PYTEST_OPTS tests/
fi

echo ""
echo -e "${GREEN}✅ All tests completed!${NC}"

# Summary
echo ""
echo -e "${BLUE}📊 Test Summary:${NC}"
echo "  - Health tests: tests/test_health.py"
echo "  - Clinic info tests: tests/test_clinic_info.py"
echo "  - Booking tests: tests/test_booking.py"
echo "  - Memory tests: tests/test_memory.py"
echo "  - Integration tests: tests/test_integration.py"
echo ""

