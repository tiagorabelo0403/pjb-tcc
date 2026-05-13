CREATE TABLE tb_atendimento_thread (
  id BIGSERIAL PRIMARY KEY,
  processo_id BIGINT NOT NULL,
  advogado_id BIGINT NOT NULL,
  cidadao_usuario_id BIGINT NOT NULL,
  cidadao_cpf_hash VARCHAR(64) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  last_message_id BIGINT,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_atendimento_thread_processo FOREIGN KEY (processo_id) REFERENCES tb_processo(id),
  CONSTRAINT fk_atendimento_thread_advogado FOREIGN KEY (advogado_id) REFERENCES tb_usuario(id),
  CONSTRAINT fk_atendimento_thread_cidadao FOREIGN KEY (cidadao_usuario_id) REFERENCES tb_usuario(id),
  CONSTRAINT uk_atendimento_thread UNIQUE (processo_id, advogado_id, cidadao_usuario_id)
);

CREATE INDEX idx_atendimento_thread_adv_updated ON tb_atendimento_thread (advogado_id, updated_at DESC);
CREATE INDEX idx_atendimento_thread_cid_updated ON tb_atendimento_thread (cidadao_usuario_id, updated_at DESC);
CREATE INDEX idx_atendimento_thread_processo ON tb_atendimento_thread (processo_id);

CREATE TABLE tb_atendimento_message (
  id BIGSERIAL PRIMARY KEY,
  thread_id BIGINT NOT NULL,
  sender_usuario_id BIGINT NOT NULL,
  sender_tipo VARCHAR(40) NOT NULL,
  body TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_atendimento_message_thread FOREIGN KEY (thread_id) REFERENCES tb_atendimento_thread(id) ON DELETE CASCADE,
  CONSTRAINT fk_atendimento_message_sender FOREIGN KEY (sender_usuario_id) REFERENCES tb_usuario(id)
);

CREATE INDEX idx_atendimento_message_thread_id ON tb_atendimento_message (thread_id, id);

CREATE TABLE tb_atendimento_read_state (
  thread_id BIGINT NOT NULL,
  usuario_id BIGINT NOT NULL,
  last_read_message_id BIGINT,
  updated_at TIMESTAMPTZ NOT NULL,
  PRIMARY KEY (thread_id, usuario_id),
  CONSTRAINT fk_atendimento_read_thread FOREIGN KEY (thread_id) REFERENCES tb_atendimento_thread(id) ON DELETE CASCADE,
  CONSTRAINT fk_atendimento_read_user FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id)
);
