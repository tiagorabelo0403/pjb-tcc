CREATE TABLE IF NOT EXISTS adv_office_process_operation (
  id BIGSERIAL PRIMARY KEY,
  equipe_id BIGINT,
  processo_id BIGINT NOT NULL,
  executor_user_id BIGINT NOT NULL,
  signer_user_id BIGINT,
  queue_item_id BIGINT,
  action_type VARCHAR(60) NOT NULL,
  status VARCHAR(40) NOT NULL,
  payload_hash VARCHAR(64) NOT NULL,
  payload_json TEXT NOT NULL,
  result_payload_json TEXT,
  executed_at TIMESTAMPTZ,
  rejected_at TIMESTAMPTZ,
  rejection_reason VARCHAR(240),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_adv_office_process_operation_equipe FOREIGN KEY (equipe_id) REFERENCES equipes(id) ON DELETE SET NULL,
  CONSTRAINT fk_adv_office_process_operation_processo FOREIGN KEY (processo_id) REFERENCES tb_processo(id) ON DELETE CASCADE,
  CONSTRAINT fk_adv_office_process_operation_executor FOREIGN KEY (executor_user_id) REFERENCES tb_usuario(id) ON DELETE CASCADE,
  CONSTRAINT fk_adv_office_process_operation_signer FOREIGN KEY (signer_user_id) REFERENCES tb_usuario(id) ON DELETE SET NULL,
  CONSTRAINT fk_adv_office_process_operation_queue FOREIGN KEY (queue_item_id) REFERENCES adv_office_signature_queue(id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_adv_office_process_operation_queue_item ON adv_office_process_operation(queue_item_id) WHERE queue_item_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_adv_office_process_operation_processo_status ON adv_office_process_operation(processo_id, status, created_at);
CREATE INDEX IF NOT EXISTS ix_adv_office_process_operation_executor_status ON adv_office_process_operation(executor_user_id, status, created_at);
CREATE INDEX IF NOT EXISTS ix_adv_office_process_operation_signer_status ON adv_office_process_operation(signer_user_id, status, created_at);
