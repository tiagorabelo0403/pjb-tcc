CREATE TABLE IF NOT EXISTS tb_atendimento_reminder (
  id BIGSERIAL PRIMARY KEY,
  thread_id BIGINT NOT NULL,
  created_by_user_id BIGINT NOT NULL,
  target_user_id BIGINT,
  body TEXT NOT NULL,
  fire_at TIMESTAMPTZ NOT NULL,
  status VARCHAR(24) NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  last_error VARCHAR(180),
  sent_message_id BIGINT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_atendimento_reminder_thread ON tb_atendimento_reminder(thread_id, fire_at);
CREATE INDEX IF NOT EXISTS idx_atendimento_reminder_due ON tb_atendimento_reminder(status, fire_at);
