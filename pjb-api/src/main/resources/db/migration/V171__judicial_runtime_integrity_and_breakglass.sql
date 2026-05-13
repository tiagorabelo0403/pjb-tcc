CREATE TABLE IF NOT EXISTS tb_federated_integrity_snapshot (
  id UUID PRIMARY KEY,
  scope_type VARCHAR(40) NOT NULL,
  scope_value VARCHAR(80) NULL,
  source_kind VARCHAR(40) NOT NULL,
  horizon_start TIMESTAMPTZ NOT NULL,
  horizon_end TIMESTAMPTZ NOT NULL,
  leaf_count INTEGER NOT NULL,
  root_hash VARCHAR(64) NOT NULL,
  previous_root_hash VARCHAR(64) NULL,
  drift_status VARCHAR(40) NOT NULL,
  payload_json TEXT NULL,
  created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_federated_integrity_snapshot_scope
  ON tb_federated_integrity_snapshot (scope_type, scope_value, source_kind, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_federated_integrity_snapshot_root
  ON tb_federated_integrity_snapshot (root_hash);

CREATE TABLE IF NOT EXISTS tb_break_glass_access_session (
  id UUID PRIMARY KEY,
  processo_id BIGINT NULL,
  nupn VARCHAR(50) NULL,
  requested_by_usuario_id BIGINT NOT NULL,
  requested_by_profile VARCHAR(60) NOT NULL,
  access_scope VARCHAR(60) NOT NULL,
  justification VARCHAR(1000) NOT NULL,
  approval_basis VARCHAR(240) NULL,
  risk_level VARCHAR(40) NOT NULL,
  status VARCHAR(40) NOT NULL,
  step_up_required BOOLEAN NOT NULL DEFAULT TRUE,
  step_up_satisfied BOOLEAN NOT NULL DEFAULT FALSE,
  expires_at TIMESTAMPTZ NULL,
  correlation_id VARCHAR(80) NULL,
  audit_hash VARCHAR(64) NOT NULL,
  metadata_json TEXT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_break_glass_processo FOREIGN KEY (processo_id) REFERENCES tb_processo(id),
  CONSTRAINT fk_break_glass_usuario FOREIGN KEY (requested_by_usuario_id) REFERENCES tb_usuario(id)
);

CREATE INDEX IF NOT EXISTS ix_break_glass_processo_created
  ON tb_break_glass_access_session (processo_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_break_glass_nupn_created
  ON tb_break_glass_access_session (nupn, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_break_glass_status_created
  ON tb_break_glass_access_session (status, created_at DESC);
