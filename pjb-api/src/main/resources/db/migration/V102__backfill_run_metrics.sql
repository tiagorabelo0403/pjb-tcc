CREATE TABLE IF NOT EXISTS pjb_backfill_run (
  job_id UUID PRIMARY KEY,
  type VARCHAR(80) NOT NULL,
  inbox_key VARCHAR(240),
  requested_by VARCHAR(120),
  batch_size INT NOT NULL DEFAULT 500,
  dry_run BOOLEAN NOT NULL DEFAULT FALSE,
  after_id BIGINT NOT NULL DEFAULT 0,
  until_id BIGINT,
  started_at TIMESTAMPTZ,
  finished_at TIMESTAMPTZ,
  processed BIGINT NOT NULL DEFAULT 0,
  updated BIGINT NOT NULL DEFAULT 0,
  duplicates BIGINT NOT NULL DEFAULT 0,
  last_cursor BIGINT NOT NULL DEFAULT 0,
  last_error TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_backfill_job FOREIGN KEY (job_id) REFERENCES tb_job(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_backfill_type_created ON pjb_backfill_run (type, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_backfill_inbox_created ON pjb_backfill_run (inbox_key, created_at DESC);
