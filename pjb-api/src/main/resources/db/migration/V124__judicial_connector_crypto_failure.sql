CREATE TABLE IF NOT EXISTS tb_judicial_connector_crypto_failure (
    id UUID PRIMARY KEY,
    connector_system VARCHAR(40) NOT NULL,
    tribunal_codigo VARCHAR(20),
    environment_name VARCHAR(60),
    operation_name VARCHAR(100) NOT NULL,
    target_scheme VARCHAR(20),
    target_host_sha256 VARCHAR(64),
    target_port INTEGER,
    failure_type VARCHAR(80) NOT NULL,
    failure_code VARCHAR(100) NOT NULL,
    failure_fingerprint VARCHAR(64) NOT NULL,
    sanitized_message VARCHAR(1000) NOT NULL,
    key_alias VARCHAR(255),
    keystore_ref VARCHAR(160),
    truststore_ref VARCHAR(160),
    tls_mode VARCHAR(20),
    correlation_id VARCHAR(200),
    metadata_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_judicial_crypto_failure_created_at ON tb_judicial_connector_crypto_failure (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_judicial_crypto_failure_system_created_at ON tb_judicial_connector_crypto_failure (connector_system, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_judicial_crypto_failure_fingerprint ON tb_judicial_connector_crypto_failure (failure_fingerprint);
