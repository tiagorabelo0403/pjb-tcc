CREATE TABLE IF NOT EXISTS tb_idempotency (
  scope VARCHAR(80) NOT NULL,
  idempotency_key VARCHAR(180) NOT NULL,
  status VARCHAR(16) NOT NULL,
  request_hash VARCHAR(96),
  response_hash VARCHAR(96),
  resource_type VARCHAR(64),
  resource_id VARCHAR(120),
  response_json TEXT,
  lock_until TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (scope, idempotency_key)
);

CREATE INDEX IF NOT EXISTS ix_idem_scope_status ON tb_idempotency (scope, status, updated_at);
CREATE INDEX IF NOT EXISTS ix_idem_lock_until ON tb_idempotency (lock_until);
CREATE INDEX IF NOT EXISTS ix_idem_created ON tb_idempotency (created_at);
