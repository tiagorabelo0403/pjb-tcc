CREATE TABLE pjb_audiencia_custodia (
 id BIGSERIAL PRIMARY KEY,
 processo_id BIGINT NOT NULL REFERENCES tb_processo(id) ON DELETE RESTRICT,
 preso_nome VARCHAR(255) NOT NULL,
 preso_cpf VARCHAR(11),
 data_prisao TIMESTAMPTZ NOT NULL,
 prazo_limite_24h TIMESTAMPTZ NOT NULL,
 status VARCHAR(32) NOT NULL DEFAULT 'PENDENTE',
 magistrado_id BIGINT REFERENCES tb_usuario(id),
 realizada_em TIMESTAMPTZ,
 resultado VARCHAR(64),
 medidas_cautelares TEXT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_custodia_status ON pjb_audiencia_custodia (status, prazo_limite_24h) WHERE status = 'PENDENTE';
CREATE INDEX idx_custodia_processo ON pjb_audiencia_custodia (processo_id);

CREATE TABLE pjb_medida_cautelar (
 id BIGSERIAL PRIMARY KEY,
 processo_id BIGINT NOT NULL REFERENCES tb_processo(id) ON DELETE RESTRICT,
 tipo VARCHAR(64) NOT NULL,
 descricao TEXT,
 periodicidade_dias INT,
 proximo_comparecimento TIMESTAMPTZ,
 ativa BOOLEAN NOT NULL DEFAULT TRUE,
 revogada_em TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_medida_ativa ON pjb_medida_cautelar (proximo_comparecimento) WHERE ativa = TRUE AND proximo_comparecimento IS NOT NULL;

CREATE TABLE pjb_bnmp_consulta_log (
 id BIGSERIAL PRIMARY KEY,
 processo_id BIGINT REFERENCES tb_processo(id) ON DELETE SET NULL,
 cpf_consultado VARCHAR(11),
 mandado_ativo BOOLEAN,
 numero_mandado VARCHAR(64),
 consultado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 operador_id BIGINT REFERENCES tb_usuario(id)
);
