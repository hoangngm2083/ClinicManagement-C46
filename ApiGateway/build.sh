#!/bin/bash
set -e

echo "Building API Gateway..."
./mvnw clean package -DskipTests

echo "API Gateway built successfully!"
