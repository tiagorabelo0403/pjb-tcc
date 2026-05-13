ALTER TABLE tb_atendimento_message
  ADD COLUMN IF NOT EXISTS client_msg_id UUID;

-- Idempotência por thread: permite o app reenviar a mesma mensagem sem duplicar.
-- Partial index (Postgres): só impõe unicidade quando client_msg_id não é nulo.
CREATE UNIQUE INDEX IF NOT EXISTS uk_atendimento_message_client_msg
  ON tb_atendimento_message (thread_id, client_msg_id)
  WHERE client_msg_id IS NOT NULL;
