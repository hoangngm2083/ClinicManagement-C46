CREATE SCHEMA IF NOT EXISTS medical_package;

CREATE TABLE IF NOT EXISTS medical_package.medical_package_view (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    price NUMERIC(15,2)
);

CREATE INDEX IF NOT EXISTS idx_package_name ON medical_package.medical_package_view (name);
CREATE INDEX IF NOT EXISTS idx_package_price ON medical_package.medical_package_view (price);
