CREATE TABLE IF NOT EXISTS tb_judicial_connector_security_session (
    id UUID PRIMARY KEY,
    connector_system VARCHAR(40) NOT NULL,
    tribunal_codigo VARCHAR(20),
    environment_name VARCHAR(60),
    operation_name VARCHAR(120) NOT NULL,
    target_scheme VARCHAR(20),
    target_host_sha256 VARCHAR(64),
    target_port INTEGER,
    tls_mode VARCHAR(20),
    outcome_status VARCHAR(60) NOT NULL,
    success BOOLEAN NOT NULL,
    http_status_code INTEGER,
    duration_millis BIGINT NOT NULL,
    hardware_backed BOOLEAN NOT NULL,
    mutual_tls BOOLEAN NOT NULL,
    hostname_verification BOOLEAN NOT NULL,
    key_store_ref VARCHAR(160),
    trust_store_ref VARCHAR(160),
    key_alias VARCHAR(255),
    correlation_id VARCHAR(200),
    metadata_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_jcss_created_at ON tb_judicial_connector_security_session (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_jcss_system_created ON tb_judicial_connector_security_session (connector_system, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_jcss_tribunal_created ON tb_judicial_connector_security_session (tribunal_codigo, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_jcss_outcome_created ON tb_judicial_connector_security_session (outcome_status, created_at DESC);
