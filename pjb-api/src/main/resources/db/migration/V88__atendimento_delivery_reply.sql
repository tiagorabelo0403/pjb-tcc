-- Estado de "entregue" (cliente confirmou recebimento).
CREATE TABLE IF NOT EXISTS tb_atendimento_delivery_state (
  thread_id BIGINT NOT NULL,
  usuario_id BIGINT NOT NULL,
  last_delivered_message_id BIGINT,
  updated_at TIMESTAMPTZ NOT NULL,
  PRIMARY KEY (thread_id, usuario_id),
  CONSTRAINT fk_atendimento_delivery_thread FOREIGN KEY (thread_id) REFERENCES tb_atendimento_thread(id) ON DELETE CASCADE,
  CONSTRAINT fk_atendimento_delivery_user FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id)
);

-- Reply-to (quote) para UX.
ALTER TABLE tb_atendimento_message
  ADD COLUMN IF NOT EXISTS reply_to_message_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_atendimento_message_reply_to
  ON tb_atendimento_message (thread_id, reply_to_message_id);
