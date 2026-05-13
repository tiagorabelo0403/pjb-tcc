-- Thread settings (por usuário) e policy (por thread): silenciar notificações, quiet hours e modo leitura.

CREATE TABLE tb_atendimento_thread_member_settings (
  thread_id BIGINT NOT NULL,
  usuario_id BIGINT NOT NULL,
  muted_until TIMESTAMPTZ,
  quiet_hours_start_min SMALLINT,
  quiet_hours_end_min SMALLINT,
  quiet_days_mask INTEGER,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  PRIMARY KEY (thread_id, usuario_id),
  CONSTRAINT fk_atendimento_tms_thread FOREIGN KEY (thread_id) REFERENCES tb_atendimento_thread(id) ON DELETE CASCADE,
  CONSTRAINT fk_atendimento_tms_user FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id)
);

CREATE INDEX idx_atendimento_tms_user ON tb_atendimento_thread_member_settings (usuario_id);

CREATE TABLE tb_atendimento_thread_policy (
  thread_id BIGINT PRIMARY KEY,
  cidadao_send_disabled_until TIMESTAMPTZ,
  updated_by_user_id BIGINT,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_atendimento_tp_thread FOREIGN KEY (thread_id) REFERENCES tb_atendimento_thread(id) ON DELETE CASCADE,
  CONSTRAINT fk_atendimento_tp_updated_by FOREIGN KEY (updated_by_user_id) REFERENCES tb_usuario(id)
);
