CREATE TABLE IF NOT EXISTS tb_comunicacao_judicial_state (
    id BIGSERIAL PRIMARY KEY,
    domain_name VARCHAR(80) NOT NULL,
    state_key VARCHAR(180) NOT NULL,
    secondary_key VARCHAR(180),
    processo_id BIGINT,
    expedicao_uuid VARCHAR(36),
    mandado_id VARCHAR(120),
    status_code VARCHAR(80),
    payload_json TEXT NOT NULL,
    hash_integridade VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT,
    CONSTRAINT uq_com_jud_state_domain_key UNIQUE (domain_name, state_key),
    CONSTRAINT fk_com_jud_state_processo FOREIGN KEY (processo_id) REFERENCES tb_processo (id)
);

CREATE INDEX IF NOT EXISTS idx_com_jud_state_domain_secondary ON tb_comunicacao_judicial_state (domain_name, secondary_key);
CREATE INDEX IF NOT EXISTS idx_com_jud_state_processo ON tb_comunicacao_judicial_state (processo_id);
CREATE INDEX IF NOT EXISTS idx_com_jud_state_expedicao ON tb_comunicacao_judicial_state (expedicao_uuid);
CREATE INDEX IF NOT EXISTS idx_com_jud_state_mandado ON tb_comunicacao_judicial_state (mandado_id);
CREATE INDEX IF NOT EXISTS idx_com_jud_state_status ON tb_comunicacao_judicial_state (domain_name, status_code);
