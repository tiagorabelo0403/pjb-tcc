CREATE TABLE IF NOT EXISTS adv_office_policy (
  id BIGSERIAL PRIMARY KEY,
  equipe_id BIGINT NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT FALSE,
  signer_user_id BIGINT,
  bloqueia_causas_proprias BOOLEAN NOT NULL DEFAULT FALSE,
  min_trust_auto INT NOT NULL DEFAULT 10,
  max_auto_por_dia INT NOT NULL DEFAULT 200,
  auto_actions VARCHAR(1200) NOT NULL DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_adv_office_policy_equipe FOREIGN KEY (equipe_id) REFERENCES equipes(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_adv_office_policy_equipe ON adv_office_policy(equipe_id);
CREATE INDEX IF NOT EXISTS ix_adv_office_policy_enabled ON adv_office_policy(enabled);

CREATE TABLE IF NOT EXISTS adv_office_delegacao_regra (
  id BIGSERIAL PRIMARY KEY,
  equipe_id BIGINT NOT NULL,
  usuario_id BIGINT NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  bloqueia_pessoal BOOLEAN NOT NULL DEFAULT FALSE,
  min_trust_auto_override INT,
  max_auto_por_dia_override INT,
  auto_actions_override VARCHAR(1200) NOT NULL DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_adv_office_regra_equipe FOREIGN KEY (equipe_id) REFERENCES equipes(id) ON DELETE CASCADE,
  CONSTRAINT fk_adv_office_regra_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_adv_office_delegacao_regra ON adv_office_delegacao_regra(equipe_id, usuario_id);
CREATE INDEX IF NOT EXISTS ix_adv_office_regra_equipe ON adv_office_delegacao_regra(equipe_id);
CREATE INDEX IF NOT EXISTS ix_adv_office_regra_usuario ON adv_office_delegacao_regra(usuario_id);

CREATE TABLE IF NOT EXISTS adv_office_delegacao_usage (
  id BIGSERIAL PRIMARY KEY,
  equipe_id BIGINT NOT NULL,
  usuario_id BIGINT NOT NULL,
  dia DATE NOT NULL,
  auto_usado INT NOT NULL DEFAULT 0,
  queue_criado INT NOT NULL DEFAULT 0,
  ultimo_evento_em TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_adv_office_usage_equipe FOREIGN KEY (equipe_id) REFERENCES equipes(id) ON DELETE CASCADE,
  CONSTRAINT fk_adv_office_usage_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_adv_office_usage ON adv_office_delegacao_usage(equipe_id, usuario_id, dia);
CREATE INDEX IF NOT EXISTS ix_adv_office_usage_equipe_dia ON adv_office_delegacao_usage(equipe_id, dia);
CREATE INDEX IF NOT EXISTS ix_adv_office_usage_usuario_dia ON adv_office_delegacao_usage(usuario_id, dia);

CREATE TABLE IF NOT EXISTS adv_office_signature_queue (
  id BIGSERIAL PRIMARY KEY,
  equipe_id BIGINT NOT NULL,
  executor_user_id BIGINT NOT NULL,
  signer_user_id BIGINT NOT NULL,
  action_type VARCHAR(60) NOT NULL,
  resource_type VARCHAR(80) NOT NULL,
  resource_id VARCHAR(80) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  request_id VARCHAR(80),
  payload_hash VARCHAR(64),
  summary VARCHAR(240),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  decided_at TIMESTAMPTZ,
  decided_by_user_id BIGINT,
  decision_reason VARCHAR(240),
  CONSTRAINT fk_office_queue_equipe FOREIGN KEY (equipe_id) REFERENCES equipes(id) ON DELETE CASCADE,
  CONSTRAINT fk_office_queue_executor FOREIGN KEY (executor_user_id) REFERENCES tb_usuario(id) ON DELETE CASCADE,
  CONSTRAINT fk_office_queue_signer FOREIGN KEY (signer_user_id) REFERENCES tb_usuario(id) ON DELETE CASCADE,
  CONSTRAINT fk_office_queue_decided_by FOREIGN KEY (decided_by_user_id) REFERENCES tb_usuario(id)
);

CREATE INDEX IF NOT EXISTS ix_office_queue_signer_status ON adv_office_signature_queue(signer_user_id, status, created_at);
CREATE INDEX IF NOT EXISTS ix_office_queue_equipe_status ON adv_office_signature_queue(equipe_id, status, created_at);
CREATE INDEX IF NOT EXISTS ix_office_queue_executor ON adv_office_signature_queue(executor_user_id, created_at);

CREATE TABLE IF NOT EXISTS adv_office_delegated_action (
  id BIGSERIAL PRIMARY KEY,
  equipe_id BIGINT NOT NULL,
  executor_user_id BIGINT NOT NULL,
  signer_user_id BIGINT NOT NULL,
  mode VARCHAR(20) NOT NULL,
  action_type VARCHAR(60) NOT NULL,
  resource_type VARCHAR(80) NOT NULL,
  resource_id VARCHAR(80) NOT NULL,
  queue_item_id BIGINT,
  job_id UUID,
  request_id VARCHAR(80),
  payload_hash VARCHAR(64),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_office_action_equipe FOREIGN KEY (equipe_id) REFERENCES equipes(id) ON DELETE CASCADE,
  CONSTRAINT fk_office_action_executor FOREIGN KEY (executor_user_id) REFERENCES tb_usuario(id) ON DELETE CASCADE,
  CONSTRAINT fk_office_action_signer FOREIGN KEY (signer_user_id) REFERENCES tb_usuario(id) ON DELETE CASCADE,
  CONSTRAINT fk_office_action_queue_item FOREIGN KEY (queue_item_id) REFERENCES adv_office_signature_queue(id)
);

CREATE INDEX IF NOT EXISTS ix_office_action_equipe_created ON adv_office_delegated_action(equipe_id, created_at);
CREATE INDEX IF NOT EXISTS ix_office_action_signer_created ON adv_office_delegated_action(signer_user_id, created_at);
CREATE INDEX IF NOT EXISTS ix_office_action_executor_created ON adv_office_delegated_action(executor_user_id, created_at);
