#!/bin/bash
set -e

echo "Creating databases: booking_db, auth_db, patient_db, medical_package_db..."

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE booking_db;
    CREATE DATABASE auth_db;
    CREATE DATABASE patient_db;
    CREATE DATABASE medical_package_db;
    CREATE DATABASE notification_db;

    -- Tạo user nếu cần (tùy chọn)
    -- CREATE USER clinic_app WITH PASSWORD 'clinic_pass';
    -- GRANT ALL PRIVILEGES ON DATABASE booking_db TO clinic_app;
EOSQL

echo "All databases created successfully!"