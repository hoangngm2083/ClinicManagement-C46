#!/bin/bash

# Script to check PostgreSQL connections status

echo "========================================="
echo "PostgreSQL Connection Status"
echo "========================================="
echo ""

# Check if docker container is running
if ! docker ps | grep -q postgres; then
    echo "❌ PostgreSQL container is not running!"
    exit 1
fi

echo "✅ PostgreSQL container is running"
echo ""

# Get max_connections setting
echo "📊 Max Connections Setting:"
docker exec postgres psql -U booking -d booking_db -c "SHOW max_connections;" -t
echo ""

# Get current total connections
echo "📊 Current Total Connections:"
TOTAL_CONN=$(docker exec postgres psql -U booking -d booking_db -t -c "SELECT count(*) FROM pg_stat_activity;")
echo "   Total: $TOTAL_CONN"
echo ""

# Get connections by database
echo "📊 Connections by Database:"
docker exec postgres psql -U booking -d booking_db -c "
SELECT 
    datname as database, 
    count(*) as connections 
FROM pg_stat_activity 
GROUP BY datname 
ORDER BY connections DESC;
"
echo ""

# Get connections by application
echo "📊 Connections by Application:"
docker exec postgres psql -U booking -d booking_db -c "
SELECT 
    COALESCE(application_name, 'unknown') as application, 
    count(*) as connections 
FROM pg_stat_activity 
WHERE application_name != '' 
GROUP BY application_name 
ORDER BY connections DESC;
"
echo ""

# Get connection states
echo "📊 Connection States:"
docker exec postgres psql -U booking -d booking_db -c "
SELECT 
    state, 
    count(*) as count 
FROM pg_stat_activity 
GROUP BY state 
ORDER BY count DESC;
"
echo ""

# Calculate usage percentage
MAX_CONN=$(docker exec postgres psql -U booking -d booking_db -t -c "SHOW max_connections;" | xargs)
USAGE=$(echo "scale=2; ($TOTAL_CONN * 100) / $MAX_CONN" | bc)

echo "📈 Connection Usage: $TOTAL_CONN / $MAX_CONN ($USAGE%)"

if (( $(echo "$USAGE > 80" | bc -l) )); then
    echo "⚠️  WARNING: Connection usage is above 80%!"
elif (( $(echo "$USAGE > 60" | bc -l) )); then
    echo "⚠️  CAUTION: Connection usage is above 60%"
else
    echo "✅ Connection usage is healthy"
fi

echo ""
echo "========================================="
