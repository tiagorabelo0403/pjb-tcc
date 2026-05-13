-- Receipts por mensagem: entregue/lido em (por destinatário)
CREATE TABLE tb_atendimento_message_receipt (
  message_id BIGINT NOT NULL,
  thread_id BIGINT NOT NULL,
  usuario_id BIGINT NOT NULL,
  delivered_at TIMESTAMPTZ,
  read_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  PRIMARY KEY (message_id, usuario_id),
  CONSTRAINT fk_atendimento_receipt_message FOREIGN KEY (message_id) REFERENCES tb_atendimento_message(id) ON DELETE CASCADE,
  CONSTRAINT fk_atendimento_receipt_thread FOREIGN KEY (thread_id) REFERENCES tb_atendimento_thread(id) ON DELETE CASCADE,
  CONSTRAINT fk_atendimento_receipt_user FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id)
);

CREATE INDEX idx_atendimento_receipt_thread_user_msg ON tb_atendimento_message_receipt (thread_id, usuario_id, message_id);

-- Checklist (documentos/prazos) dentro do thread
CREATE TABLE tb_atendimento_checklist_item (
  id BIGSERIAL PRIMARY KEY,
  thread_id BIGINT NOT NULL,
  kind VARCHAR(16) NOT NULL,
  status VARCHAR(16) NOT NULL,
  title VARCHAR(200) NOT NULL,
  note VARCHAR(800),
  due_at TIMESTAMPTZ,
  documento_id BIGINT,
  created_by_user_id BIGINT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  completed_at TIMESTAMPTZ,
  completed_by_user_id BIGINT,
  cancelled_at TIMESTAMPTZ,
  cancelled_by_user_id BIGINT,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_atendimento_chk_thread FOREIGN KEY (thread_id) REFERENCES tb_atendimento_thread(id) ON DELETE CASCADE,
  CONSTRAINT fk_atendimento_chk_created_by FOREIGN KEY (created_by_user_id) REFERENCES tb_usuario(id),
  CONSTRAINT fk_atendimento_chk_completed_by FOREIGN KEY (completed_by_user_id) REFERENCES tb_usuario(id),
  CONSTRAINT fk_atendimento_chk_cancelled_by FOREIGN KEY (cancelled_by_user_id) REFERENCES tb_usuario(id)
);

CREATE INDEX idx_atendimento_chk_thread ON tb_atendimento_checklist_item (thread_id, id);
CREATE INDEX idx_atendimento_chk_thread_status ON tb_atendimento_checklist_item (thread_id, status);

-- Audit chain do checklist (integridade)
CREATE TABLE tb_atendimento_checklist_audit (
  id BIGSERIAL PRIMARY KEY,
  thread_id BIGINT NOT NULL,
  item_id BIGINT NOT NULL,
  actor_user_id BIGINT NOT NULL,
  event_type VARCHAR(20) NOT NULL,
  payload_json TEXT,
  payload_hash VARCHAR(64) NOT NULL,
  prev_hash VARCHAR(64),
  chain_hash VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_atendimento_chk_audit_thread FOREIGN KEY (thread_id) REFERENCES tb_atendimento_thread(id) ON DELETE CASCADE,
  CONSTRAINT fk_atendimento_chk_audit_item FOREIGN KEY (item_id) REFERENCES tb_atendimento_checklist_item(id) ON DELETE CASCADE,
  CONSTRAINT fk_atendimento_chk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES tb_usuario(id)
);

CREATE INDEX idx_atendimento_chk_audit_thread ON tb_atendimento_checklist_audit (thread_id, id);
CREATE INDEX idx_atendimento_chk_audit_item ON tb_atendimento_checklist_audit (item_id, id);
