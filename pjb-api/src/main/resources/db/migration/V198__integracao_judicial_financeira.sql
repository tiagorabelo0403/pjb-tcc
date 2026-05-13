CREATE TABLE pjb_sisbajud_operacao (
 id BIGSERIAL PRIMARY KEY,
 processo_id BIGINT NOT NULL REFERENCES tb_processo(id) ON DELETE RESTRICT,
 tipo VARCHAR(32) NOT NULL,
 valor_solicitado NUMERIC(19,2),
 numero_oficio VARCHAR(64),
 protocolo_bacen VARCHAR(128),
 status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
 retorno_bacen TEXT,
 tentativas INT NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 confirmado_em TIMESTAMPTZ,
 operador_id BIGINT REFERENCES tb_usuario(id),
 authz_trail_id VARCHAR(128)
);
CREATE INDEX idx_sisbajud_processo ON pjb_sisbajud_operacao (processo_id);
CREATE INDEX idx_sisbajud_status ON pjb_sisbajud_operacao (status) WHERE status IN ('PENDING','FAILED');

CREATE TABLE pjb_renajud_restricao (
 id BIGSERIAL PRIMARY KEY,
 processo_id BIGINT NOT NULL REFERENCES tb_processo(id) ON DELETE RESTRICT,
 tipo VARCHAR(32) NOT NULL,
 placa VARCHAR(16),
 renavam VARCHAR(11),
 protocolo_denatran VARCHAR(128),
 status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 operador_id BIGINT REFERENCES tb_usuario(id),
 authz_trail_id VARCHAR(128)
);
CREATE INDEX idx_renajud_processo ON pjb_renajud_restricao (processo_id);
CREATE INDEX idx_renajud_status ON pjb_renajud_restricao (status) WHERE status IN ('PENDING','FAILED');

CREATE TABLE pjb_infojud_consulta (
 id BIGSERIAL PRIMARY KEY,
 processo_id BIGINT NOT NULL REFERENCES tb_processo(id) ON DELETE RESTRICT,
 cpf_cnpj_consultado VARCHAR(14) NOT NULL,
 protocolo_receita VARCHAR(128),
 status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
 resumo_retorno TEXT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 confirmado_em TIMESTAMPTZ,
 operador_id BIGINT REFERENCES tb_usuario(id),
 authz_trail_id VARCHAR(128)
);
CREATE INDEX idx_infojud_processo ON pjb_infojud_consulta (processo_id);
CREATE INDEX idx_infojud_status ON pjb_infojud_consulta (status) WHERE status IN ('PENDING','FAILED');
