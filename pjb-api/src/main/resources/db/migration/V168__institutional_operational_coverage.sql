CREATE TABLE IF NOT EXISTS tb_inst_operational_coverage_rule_snapshot (
    id BIGSERIAL PRIMARY KEY,
    ver BIGINT NOT NULL DEFAULT 0,
    rule_id VARCHAR(160) NOT NULL,
    unidade_codigo VARCHAR(160) NOT NULL,
    caixa_codigo VARCHAR(160) NOT NULL,
    titular_usuario_id BIGINT NOT NULL,
    cobertura_usuario_id BIGINT NOT NULL,
    status_codigo VARCHAR(80) NOT NULL,
    tipo_cobertura VARCHAR(80) NOT NULL,
    hash_integridade VARCHAR(128) NOT NULL,
    snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_inst_cov_rule_rule_id UNIQUE (rule_id)
);

CREATE INDEX IF NOT EXISTS idx_inst_cov_rule_unidade ON tb_inst_operational_coverage_rule_snapshot (unidade_codigo, updated_at);
CREATE INDEX IF NOT EXISTS idx_inst_cov_rule_caixa ON tb_inst_operational_coverage_rule_snapshot (caixa_codigo, updated_at);
CREATE INDEX IF NOT EXISTS idx_inst_cov_rule_status ON tb_inst_operational_coverage_rule_snapshot (status_codigo, updated_at);
