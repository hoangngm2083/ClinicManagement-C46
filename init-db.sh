#!/bin/bash
set -e

echo "Creating databases: booking_db, auth_db, patient_db, medical_package_db, vector_db..."

# Kết nối đến database mặc định (postgres) để tạo các database khác
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" <<-EOSQL
    CREATE DATABASE booking_db;
    CREATE DATABASE auth_db;
    CREATE DATABASE patient_db;
    CREATE DATABASE medical_package_db;
    CREATE DATABASE notification_db;
    CREATE DATABASE staff_db;
    CREATE DATABASE examination_db;
    CREATE DATABASE examination_flow_db;
    CREATE DATABASE payment_db;
    CREATE DATABASE vector_db;
EOSQL

echo "All databases created successfully!"