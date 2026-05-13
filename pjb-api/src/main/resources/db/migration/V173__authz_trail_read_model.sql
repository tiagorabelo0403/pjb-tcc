CREATE TABLE IF NOT EXISTS tb_authz_trail_read_model (
    id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    audit_event_code VARCHAR(120) NOT NULL,
    action VARCHAR(120) NOT NULL,
    resource_type VARCHAR(120) NOT NULL,
    resource_id VARCHAR(240) NOT NULL,
    allowed BOOLEAN NOT NULL,
    reason VARCHAR(160) NOT NULL,
    policy_version VARCHAR(80) NOT NULL,
    policy_descriptor_sha256 VARCHAR(128) NOT NULL,
    actor_id BIGINT,
    actor_type VARCHAR(80) NOT NULL,
    request_id VARCHAR(120) NOT NULL,
    justificativa TEXT NOT NULL,
    effective_sigilo VARCHAR(80) NOT NULL,
    risk_level VARCHAR(40) NOT NULL,
    risk_score INTEGER NOT NULL,
    step_up_channel VARCHAR(80) NOT NULL,
    step_up_code VARCHAR(120) NOT NULL,
    step_up_required BOOLEAN NOT NULL,
    step_up_satisfied BOOLEAN NOT NULL,
    governance_channel VARCHAR(80) NOT NULL,
    governance_code VARCHAR(120) NOT NULL,
    governance_scope VARCHAR(120) NOT NULL,
    governance_required BOOLEAN NOT NULL,
    governance_satisfied BOOLEAN NOT NULL,
    integration_code VARCHAR(120) NOT NULL,
    institutional_unit_code VARCHAR(120) NOT NULL,
    institutional_box_code VARCHAR(120) NOT NULL,
    institutional_capability_code VARCHAR(120) NOT NULL,
    expedicao_uuid VARCHAR(120) NOT NULL,
    payload_hash VARCHAR(128) NOT NULL UNIQUE,
    audit_description TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_authz_trail_occurred ON tb_authz_trail_read_model (occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_authz_trail_action_resource ON tb_authz_trail_read_model (action, resource_type, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_authz_trail_actor ON tb_authz_trail_read_model (actor_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_authz_trail_request ON tb_authz_trail_read_model (request_id);
CREATE INDEX IF NOT EXISTS idx_authz_trail_integration ON tb_authz_trail_read_model (integration_code, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_authz_trail_institutional ON tb_authz_trail_read_model (institutional_unit_code, institutional_box_code, institutional_capability_code, occurred_at DESC);

INSERT INTO tb_database_retention_policy (table_name, retention_window_days, archive_strategy, purge_mode, legal_hold_supported, notes)
VALUES ('tb_authz_trail_read_model', 3650, 'KEEP_PRIMARY_AUDIT', 'NO_PURGE', TRUE, 'Read model persistente da trilha de autorizacao ABAC para auditoria operacional e forense.')
ON CONFLICT (table_name)
DO UPDATE SET
    retention_window_days = EXCLUDED.retention_window_days,
    archive_strategy = EXCLUDED.archive_strategy,
    purge_mode = EXCLUDED.purge_mode,
    legal_hold_supported = EXCLUDED.legal_hold_supported,
    notes = EXCLUDED.notes,
    updated_at = NOW();
