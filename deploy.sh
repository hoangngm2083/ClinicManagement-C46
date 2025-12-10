#!/bin/bash

# Clinic Management System - Deploy Script
# Usage: ./deploy.sh [docker-compose arguments]
# Example: ./deploy.sh up --build
#          ./deploy.sh down
#          ./deploy.sh logs -f

set -e

# Check if docker is available
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

# Determine which docker compose command to use
# Check if docker compose (v2) is available, otherwise fall back to docker-compose (v1)
if docker compose version &>/dev/null; then
    COMPOSE_CMD="docker compose"
    echo "🐳 Using Docker Compose V2 (docker compose)"
else
    COMPOSE_CMD="docker-compose"
    echo "🐳 Using Docker Compose V1 (docker-compose)"
fi

# Default compose file and env file
COMPOSE_FILE="docker-compose.deploy.yml"
ENV_FILE=".env.prod"

# Get all arguments passed to the script
ARGS="$@"

# If no arguments provided, show usage
if [ $# -eq 0 ]; then
    echo "🚀 Clinic Management System - Deploy Script"
    echo ""
    echo "Usage: ./deploy.sh [docker-compose arguments]"
    echo ""
    echo "Examples:"
    echo "  ./deploy.sh up --build          # Build and start services"
    echo "  ./deploy.sh up -d                # Start services in background"
    echo "  ./deploy.sh down                 # Stop and remove services"
    echo "  ./deploy.sh logs -f              # View logs"
    echo "  ./deploy.sh ps                   # Show service status"
    echo ""
    echo "Default configuration:"
    echo "  - Compose file: $COMPOSE_FILE"
    echo "  - Env file: $ENV_FILE"
    exit 0
fi

echo "🚀 Executing: $COMPOSE_CMD -f $COMPOSE_FILE --env-file $ENV_FILE $ARGS"

# Execute the docker compose command with all arguments
exec $COMPOSE_CMD -f "$COMPOSE_FILE" --env-file "$ENV_FILE" $ARGS
