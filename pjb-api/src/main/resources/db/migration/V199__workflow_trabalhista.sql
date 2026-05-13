CREATE TABLE pjb_gru_judicial_trabalhista (
 id BIGSERIAL PRIMARY KEY,
 processo_id BIGINT NOT NULL REFERENCES tb_processo(id) ON DELETE RESTRICT,
 tipo VARCHAR(64) NOT NULL,
 valor NUMERIC(19,2) NOT NULL,
 indice_atualizacao VARCHAR(32),
 nosso_numero VARCHAR(64),
 linha_digitavel VARCHAR(120),
 status VARCHAR(32) NOT NULL DEFAULT 'PENDENTE',
 vencimento DATE,
 pago_em TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 tribunal_trt VARCHAR(16)
);
CREATE INDEX idx_gru_trab_processo ON pjb_gru_judicial_trabalhista (processo_id);

CREATE TABLE pjb_deposito_recursal (
 id BIGSERIAL PRIMARY KEY,
 processo_id BIGINT NOT NULL REFERENCES tb_processo(id) ON DELETE RESTRICT,
 instancia VARCHAR(16) NOT NULL,
 valor_teto NUMERIC(19,2),
 valor_depositado NUMERIC(19,2),
 data_deposito DATE,
 comprovante_hash VARCHAR(128),
 status VARCHAR(32) NOT NULL DEFAULT 'PENDENTE',
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_deposito_recursal_processo ON pjb_deposito_recursal (processo_id);
