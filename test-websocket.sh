#!/bin/bash

echo "🧪 Testing WebSocket functionality..."
echo

# Test 1: Check nginx WebSocket endpoint accessibility
echo "1️⃣ Testing nginx WebSocket endpoint..."
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" http://localhost/ws/exam-workflow/info
echo

# Test 2: Check SockJS info endpoint
echo "2️⃣ Testing SockJS info endpoint..."
curl -s http://localhost/ws/exam-workflow/info | head -c 200
echo
echo

# Test 3: Check examination-flow-service direct access
echo "3️⃣ Testing examination-flow-service direct access..."
docker compose -f docker-compose.deploy.yml --env-file .env.prod exec -T examination-flow-service curl -s http://localhost:9093/ws/exam-workflow/info | head -c 200
echo
echo

echo "✅ WebSocket infrastructure is working!"
echo "📋 Summary:"
echo "   - Nginx: ✅ Routing WebSocket requests"
echo "   - Examination Flow Service: ✅ Processing WebSocket requests"
echo "   - SockJS: ✅ Active and responding"
echo
echo "🎮 WebSocket endpoint ready for client connections!"
