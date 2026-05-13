CREATE TABLE IF NOT EXISTS tb_judicial_connector_certificate_inventory (
    id UUID PRIMARY KEY,
    connector_system VARCHAR(40) NOT NULL,
    tribunal_codigo VARCHAR(20),
    environment_name VARCHAR(60) NOT NULL,
    binding_id VARCHAR(120) NOT NULL,
    target_uri VARCHAR(1000),
    keystore_ref VARCHAR(160),
    truststore_ref VARCHAR(160),
    key_alias VARCHAR(255),
    tls_mode VARCHAR(20),
    certificate_present BOOLEAN NOT NULL DEFAULT FALSE,
    hardware_backed BOOLEAN NOT NULL DEFAULT FALSE,
    valid_now BOOLEAN NOT NULL DEFAULT FALSE,
    expires_soon BOOLEAN NOT NULL DEFAULT FALSE,
    expired BOOLEAN NOT NULL DEFAULT FALSE,
    truststore_present BOOLEAN NOT NULL DEFAULT FALSE,
    path_validation_succeeded BOOLEAN NOT NULL DEFAULT FALSE,
    revocation_attempted BOOLEAN NOT NULL DEFAULT FALSE,
    revocation_soft_failed BOOLEAN NOT NULL DEFAULT FALSE,
    revocation_hard_failed BOOLEAN NOT NULL DEFAULT FALSE,
    validation_status VARCHAR(40) NOT NULL,
    not_before TIMESTAMP,
    not_after TIMESTAMP,
    remaining_validity_seconds BIGINT,
    certificate_chain_length INTEGER NOT NULL DEFAULT 0,
    subject_dn VARCHAR(1000),
    issuer_dn VARCHAR(1000),
    serial_number_hex VARCHAR(256),
    sha256_fingerprint VARCHAR(128),
    blockers_json TEXT,
    warnings_json TEXT,
    metadata_json TEXT,
    last_validated_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_judicial_connector_certificate_inventory_identity
    ON tb_judicial_connector_certificate_inventory (connector_system, tribunal_codigo, environment_name, binding_id);

CREATE INDEX IF NOT EXISTS idx_judicial_connector_certificate_inventory_status
    ON tb_judicial_connector_certificate_inventory (validation_status, expired, expires_soon, last_validated_at);

CREATE INDEX IF NOT EXISTS idx_judicial_connector_certificate_inventory_system_tribunal
    ON tb_judicial_connector_certificate_inventory (connector_system, tribunal_codigo);
