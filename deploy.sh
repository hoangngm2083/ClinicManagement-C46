#!/bin/bash

# Clinic Management System - Deploy Script
# Usage: ./deploy.sh [dev|prod]

set -e

ENVIRONMENT=${1:-prod}
COMPOSE_FILE="docker-compose.${ENVIRONMENT}.yml"
ENV_FILE=".env.prod"

if [ "$ENVIRONMENT" = "dev" ]; then
    COMPOSE_FILE="docker-compose.dev.yml"
    ENV_FILE=""
fi

echo "🚀 Deploying Clinic Management System - $ENVIRONMENT Environment"
echo "📄 Using compose file: $COMPOSE_FILE"

# Check if docker and docker-compose are available
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Pull latest images
echo "📥 Pulling latest images..."
if [ "$ENVIRONMENT" = "prod" ]; then
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" pull
else
    docker-compose -f "$COMPOSE_FILE" pull
fi

# Start services
echo "🏃 Starting services..."
if [ "$ENVIRONMENT" = "prod" ]; then
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d
else
    docker-compose -f "$COMPOSE_FILE" up -d
fi

# Wait for services to be healthy
echo "⏳ Waiting for services to be healthy..."
sleep 30

# Check status
echo "📊 Checking service status..."
if [ "$ENVIRONMENT" = "prod" ]; then
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps
else
    docker-compose -f "$COMPOSE_FILE" ps
fi

# Check health endpoint
echo "🏥 Checking health endpoint..."
if curl -f http://localhost/health &>/dev/null; then
    echo "✅ Health check passed!"
    echo ""
    echo "🎉 Deployment successful!"
    echo ""
    echo "🌐 Application URLs:"
    if [ "$ENVIRONMENT" = "prod" ]; then
        echo "   - API Gateway: http://your-server-ip"
        echo "   - Health Check: http://your-server-ip/health"
    else
        echo "   - API Gateway: http://localhost:8080"
        echo "   - Auth Service: http://localhost:8081"
        echo "   - Booking Service: http://localhost:8082"
        echo "   - Notification Service: http://localhost:8083"
        echo "   - Patient Service: http://localhost:8088"
        echo "   - Staff Service: http://localhost:8090"
        echo "   - Medical Package Service: http://localhost:8086"
        echo "   - Examination Service: http://localhost:9094"
        echo "   - Examination Flow Service: http://localhost:9093"
        echo "   - Payment Service: http://localhost:9098"
        echo "   - AI Service: http://localhost:8000"
        echo "   - Axon Server Dashboard: http://localhost:8024"
        echo "   - PostgreSQL: localhost:5432"
        echo "   - Redis: localhost:6379"
    fi
    echo ""
    echo "📋 Useful commands:"
    echo "   - View logs: docker-compose -f $COMPOSE_FILE logs -f"
    echo "   - Stop services: docker-compose -f $COMPOSE_FILE down"
    echo "   - Restart service: docker-compose -f $COMPOSE_FILE restart <service-name>"
else
    echo "⚠️  Health check failed. Checking service logs..."
    if [ "$ENVIRONMENT" = "prod" ]; then
        docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs --tail=50
    else
        docker-compose -f "$COMPOSE_FILE" logs --tail=50
    fi
    echo ""
    echo "🔍 Check the logs above for any errors."
    exit 1
fi
