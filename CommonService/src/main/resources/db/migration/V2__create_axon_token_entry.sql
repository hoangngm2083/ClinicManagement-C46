-- V2__create_axon_token_entry.sql
CREATE TABLE IF NOT EXISTS token_entry (
    processor_name VARCHAR(255) NOT NULL,
    segment INTEGER NOT NULL,
    token BYTEA,
    token_type VARCHAR(255),
    timestamp VARCHAR(255),
    owner VARCHAR(255),
    PRIMARY KEY (processor_name, segment)
);

CREATE INDEX IF NOT EXISTS ix_token_entry_processor ON token_entry(processor_name);