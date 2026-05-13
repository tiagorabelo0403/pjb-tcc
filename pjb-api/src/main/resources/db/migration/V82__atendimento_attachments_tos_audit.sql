CREATE TABLE IF NOT EXISTS tb_atendimento_attachment (
  id BIGSERIAL PRIMARY KEY,
  thread_id BIGINT NOT NULL,
  uploader_user_id BIGINT NOT NULL,
  storage_key VARCHAR(256) NOT NULL,
  file_name VARCHAR(180) NOT NULL,
  content_type VARCHAR(80) NOT NULL,
  size_bytes BIGINT NOT NULL,
  sha256 VARCHAR(64),
  status VARCHAR(24) NOT NULL,
  rejection_reason VARCHAR(64),
  created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_att_thread ON tb_atendimento_attachment(thread_id);
CREATE INDEX IF NOT EXISTS idx_att_status ON tb_atendimento_attachment(status);

CREATE TABLE IF NOT EXISTS tb_atendimento_message_attachment (
  message_id BIGINT NOT NULL,
  attachment_id BIGINT NOT NULL,
  thread_id BIGINT NOT NULL,
  PRIMARY KEY (message_id, attachment_id)
);

CREATE INDEX IF NOT EXISTS idx_att_msg_att_msg ON tb_atendimento_message_attachment(message_id);
CREATE INDEX IF NOT EXISTS idx_att_msg_att_att ON tb_atendimento_message_attachment(attachment_id);

ALTER TABLE tb_atendimento_message ADD COLUMN IF NOT EXISTS prev_hash VARCHAR(64);
ALTER TABLE tb_atendimento_message ADD COLUMN IF NOT EXISTS msg_hash VARCHAR(64);
UPDATE tb_atendimento_message SET msg_hash = COALESCE(msg_hash, '');
ALTER TABLE tb_atendimento_message ALTER COLUMN msg_hash SET NOT NULL;

CREATE TABLE IF NOT EXISTS tb_atendimento_tos_acceptance (
  usuario_id BIGINT PRIMARY KEY,
  version INT NOT NULL,
  accepted_at TIMESTAMPTZ NOT NULL
);
