CREATE TABLE IF NOT EXISTS tb_atendimento_moderation_event (
  id BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMPTZ NOT NULL,
  actor_user_id BIGINT NOT NULL,
  actor_tipo VARCHAR(40) NOT NULL,
  thread_id BIGINT,
  processo_id BIGINT,
  reason VARCHAR(64) NOT NULL,
  content_hash VARCHAR(64) NOT NULL,
  snippet VARCHAR(200),
  metadata_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_att_mod_event_at ON tb_atendimento_moderation_event (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_att_mod_event_actor ON tb_atendimento_moderation_event (actor_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_att_mod_event_thread ON tb_atendimento_moderation_event (thread_id, created_at DESC);
