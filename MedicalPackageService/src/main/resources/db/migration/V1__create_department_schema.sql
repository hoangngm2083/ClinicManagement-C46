CREATE SCHEMA IF NOT EXISTS department;

CREATE TABLE IF NOT EXISTS department.department_view (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255),
    code VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_department_name ON department.department_view (name);
CREATE INDEX IF NOT EXISTS idx_department_code ON department.department_view (code);
