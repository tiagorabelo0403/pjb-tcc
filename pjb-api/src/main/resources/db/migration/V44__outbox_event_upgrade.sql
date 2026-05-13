ALTER TABLE tb_outbox_event
  ADD COLUMN IF NOT EXISTS dedup_key VARCHAR(200),
  ADD COLUMN IF NOT EXISTS aggregate_type VARCHAR(120),
  ADD COLUMN IF NOT EXISTS aggregate_id VARCHAR(180),
  ADD COLUMN IF NOT EXISTS headers_json TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS uk_outbox_dedup ON tb_outbox_event (dedup_key) WHERE dedup_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_outbox_aggregate ON tb_outbox_event (aggregate_type, aggregate_id);
