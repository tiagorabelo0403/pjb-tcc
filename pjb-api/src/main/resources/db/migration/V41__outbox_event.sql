CREATE TABLE IF NOT EXISTS tb_outbox_event (
  id UUID PRIMARY KEY,
  routing_key VARCHAR(180) NOT NULL,
  event_type VARCHAR(120) NOT NULL,
  payload_json TEXT NOT NULL,
  status VARCHAR(20) NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  available_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  locked_by VARCHAR(120),
  locked_at TIMESTAMPTZ,
  last_error TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_outbox_status_available ON tb_outbox_event (status, available_at, created_at);
CREATE INDEX IF NOT EXISTS ix_outbox_routing_key ON tb_outbox_event (routing_key);
