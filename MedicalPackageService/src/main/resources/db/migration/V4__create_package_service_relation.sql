CREATE TABLE IF NOT EXISTS medical_package.package_service_relation_view (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    medical_package_id VARCHAR(64) NOT NULL,
    medical_service_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),

    CONSTRAINT fk_pkg_rel_package FOREIGN KEY (medical_package_id)
        REFERENCES medical_package.medical_package_view(id)
        ON UPDATE CASCADE ON DELETE CASCADE,

    CONSTRAINT fk_pkg_rel_service FOREIGN KEY (medical_service_id)
        REFERENCES medical_service.medical_service_view(id)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_package_service_unique
    ON medical_package.package_service_relation_view (medical_package_id, medical_service_id);

CREATE INDEX IF NOT EXISTS idx_package_service_pkg ON medical_package.package_service_relation_view (medical_package_id);
CREATE INDEX IF NOT EXISTS idx_package_service_svc ON medical_package.package_service_relation_view (medical_service_id);
