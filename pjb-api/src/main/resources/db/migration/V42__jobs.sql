CREATE TABLE IF NOT EXISTS tb_job (
  id UUID PRIMARY KEY,
  type VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL,
  priority INT NOT NULL DEFAULT 0,
  inbox_key VARCHAR(240),
  owner_user_id VARCHAR(120),
  idempotency_key VARCHAR(180),
  input_json TEXT,
  progress_current BIGINT NOT NULL DEFAULT 0,
  progress_total BIGINT NOT NULL DEFAULT 0,
  attempts INT NOT NULL DEFAULT 0,
  max_attempts INT NOT NULL DEFAULT 10,
  next_retry_at TIMESTAMPTZ,
  last_error TEXT,
  locked_by VARCHAR(120),
  locked_at TIMESTAMPTZ,
  paused_at TIMESTAMPTZ,
  pause_reason VARCHAR(240),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_job_type_idem ON tb_job (type, idempotency_key);
CREATE INDEX IF NOT EXISTS ix_job_status_next ON tb_job (status, next_retry_at);
CREATE INDEX IF NOT EXISTS ix_job_inbox_status ON tb_job (inbox_key, status);
CREATE INDEX IF NOT EXISTS ix_job_owner_created ON tb_job (owner_user_id, created_at);
CREATE INDEX IF NOT EXISTS ix_job_claim ON tb_job (status, next_retry_at, priority DESC, created_at);

CREATE TABLE IF NOT EXISTS tb_job_item (
  id UUID PRIMARY KEY,
  job_id UUID NOT NULL,
  item_key VARCHAR(240) NOT NULL,
  status VARCHAR(16) NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  max_attempts INT NOT NULL DEFAULT 10,
  last_error TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_job_item_job FOREIGN KEY (job_id) REFERENCES tb_job (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_job_item_job_key ON tb_job_item (job_id, item_key);
CREATE INDEX IF NOT EXISTS ix_job_item_job_status ON tb_job_item (job_id, status);
