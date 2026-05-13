CREATE TABLE IF NOT EXISTS tb_user_calendar_system_event (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  processo_id BIGINT NULL,
  domain_key VARCHAR(180) NOT NULL,
  event_type VARCHAR(40) NOT NULL,
  title VARCHAR(180) NOT NULL,
  body VARCHAR(4000) NULL,
  at TIMESTAMP NOT NULL,
  color VARCHAR(16) NOT NULL,
  details_url VARCHAR(255) NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  UNIQUE (usuario_id, domain_key)
);

CREATE INDEX IF NOT EXISTS idx_ucse_user_at ON tb_user_calendar_system_event (usuario_id, at);
CREATE INDEX IF NOT EXISTS idx_ucse_user_proc ON tb_user_calendar_system_event (usuario_id, processo_id);
