CREATE TABLE IF NOT EXISTS tb_judicial_connector_policy (
    id UUID PRIMARY KEY,
    connector_system VARCHAR(40) NOT NULL,
    environment_name VARCHAR(60),
    tribunal_codigo VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    production_ready BOOLEAN,
    tribunal_homologated BOOLEAN,
    tribunal_blocked BOOLEAN,
    quarantine_enabled BOOLEAN,
    maintenance_mode BOOLEAN,
    contract_version VARCHAR(80),
    certificate_alias VARCHAR(255),
    submit_path VARCHAR(500),
    dry_run_path VARCHAR(500),
    snapshot_path VARCHAR(500),
    events_path VARCHAR(500),
    rollout_state VARCHAR(80),
    approved_by VARCHAR(160),
    reason VARCHAR(1000),
    notes TEXT,
    valid_from TIMESTAMPTZ,
    valid_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_judicial_connector_policy_scope ON tb_judicial_connector_policy (connector_system, environment_name, tribunal_codigo, active);
CREATE INDEX IF NOT EXISTS idx_judicial_connector_policy_validity ON tb_judicial_connector_policy (valid_until, updated_at);
CREATE TABLE IF NOT EXISTS tb_judicial_connector_admin_operation (
    id UUID PRIMARY KEY,
    connector_system VARCHAR(40) NOT NULL,
    tribunal_codigo VARCHAR(20),
    environment_name VARCHAR(60),
    operation_type VARCHAR(80) NOT NULL,
    requested_by VARCHAR(160),
    reason VARCHAR(1000),
    payload_json TEXT,
    outcome_status VARCHAR(80) NOT NULL,
    outcome_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_judicial_connector_admin_operation_created ON tb_judicial_connector_admin_operation (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_judicial_connector_admin_operation_scope ON tb_judicial_connector_admin_operation (connector_system, tribunal_codigo, environment_name);
