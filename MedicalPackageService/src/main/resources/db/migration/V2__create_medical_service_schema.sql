CREATE SCHEMA IF NOT EXISTS medical_service;

CREATE TABLE IF NOT EXISTS medical_service.medical_service_view (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    department_id VARCHAR(64),
    CONSTRAINT fk_service_department FOREIGN KEY (department_id)
        REFERENCES department.department_view(id)
        ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_service_name ON medical_service.medical_service_view (name);
CREATE INDEX IF NOT EXISTS idx_service_department_id ON medical_service.medical_service_view (department_id);
