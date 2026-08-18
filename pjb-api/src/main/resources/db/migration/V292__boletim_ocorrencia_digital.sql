CREATE TABLE IF NOT EXISTS tb_boletim_ocorrencia_digital (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    numero_boletim VARCHAR(80) NOT NULL UNIQUE,
    status VARCHAR(40) NOT NULL,
    natureza_fato VARCHAR(180) NOT NULL,
    resumo_fatos TEXT NOT NULL,
    local_fato VARCHAR(255) NOT NULL,
    ocorrido_em TIMESTAMPTZ NOT NULL,
    comunicante_resumo TEXT NOT NULL,
    envolvidos_resumo TEXT NOT NULL DEFAULT '',
    providencias_iniciais TEXT NOT NULL,
    unidade_registro_id BIGINT NOT NULL REFERENCES tb_unidade_institucional(id),
    registrado_por_id BIGINT NOT NULL REFERENCES tb_usuario(id),
    cadeia_custodia_hash VARCHAR(128) NOT NULL,
    registrado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_boletim_ocorrencia_status CHECK (status IN ('REGISTRADO', 'VINCULADO_INQUERITO'))
);

CREATE INDEX IF NOT EXISTS idx_boletim_ocorrencia_unidade_status
    ON tb_boletim_ocorrencia_digital (unidade_registro_id, status, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_boletim_ocorrencia_registrado_por
    ON tb_boletim_ocorrencia_digital (registrado_por_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS tb_boletim_ocorrencia_inquerito_vinculo (
    id BIGSERIAL PRIMARY KEY,
    boletim_id BIGINT NOT NULL REFERENCES tb_boletim_ocorrencia_digital(id) ON DELETE RESTRICT,
    inquerito_id BIGINT NOT NULL REFERENCES tb_inquerito_policial_digital(id) ON DELETE RESTRICT,
    vinculado_por_id BIGINT NOT NULL REFERENCES tb_usuario(id),
    vinculado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    cadeia_custodia_hash VARCHAR(128) NOT NULL,
    CONSTRAINT uq_boletim_ocorrencia_vinculo_boletim UNIQUE (boletim_id)
);

CREATE INDEX IF NOT EXISTS idx_boletim_ocorrencia_vinculo_inquerito
    ON tb_boletim_ocorrencia_inquerito_vinculo (inquerito_id, vinculado_em DESC);
