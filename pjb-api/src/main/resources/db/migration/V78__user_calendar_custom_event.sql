CREATE TABLE tb_user_calendar_custom_event (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  processo_id BIGINT NULL,
  title VARCHAR(180) NOT NULL,
  at TIMESTAMP NOT NULL,
  color VARCHAR(16) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_ucce_user_at ON tb_user_calendar_custom_event (usuario_id, at);
CREATE INDEX idx_ucce_user_proc ON tb_user_calendar_custom_event (usuario_id, processo_id);
