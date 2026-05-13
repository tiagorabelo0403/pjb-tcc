CREATE TABLE IF NOT EXISTS tb_user_calendar_marker (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  event_type VARCHAR(24) NOT NULL,
  event_id BIGINT NOT NULL,
  color VARCHAR(16) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (usuario_id, event_type, event_id)
);

CREATE INDEX IF NOT EXISTS ix_calmark_user_event ON tb_user_calendar_marker (usuario_id, event_type, event_id);
