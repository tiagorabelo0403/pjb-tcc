CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE IF NOT EXISTS tb_jurisdicao_territorial (
    id BIGSERIAL PRIMARY KEY,
    municipio_ibge VARCHAR(7) NOT NULL,
    municipio_nome VARCHAR(120) NOT NULL,
    uf VARCHAR(2) NOT NULL,
    tipo_justica VARCHAR(30) NOT NULL,
    modo_competencia VARCHAR(30) NOT NULL,
    unidade_codigo VARCHAR(80) NOT NULL,
    tribunal_codigo VARCHAR(20) NOT NULL,
    fonte_normativa VARCHAR(240) NOT NULL,
    vigencia_inicio DATE NOT NULL,
    vigencia_fim DATE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_jurisdicao_ibge CHECK (municipio_ibge ~ '^[0-9]{7}$'),
    CONSTRAINT ck_jurisdicao_uf CHECK (uf ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_jurisdicao_tipo_justica CHECK (tipo_justica IN
        ('ESTADUAL', 'FEDERAL', 'ELEITORAL', 'MILITAR_ESTADUAL', 'MILITAR_FEDERAL', 'TRABALHO', 'SUPERIOR')),
    CONSTRAINT ck_jurisdicao_modo CHECK (modo_competencia IN ('ORIGINARIA', 'DELEGADA_JUIZ_DIREITO')),
    CONSTRAINT ck_jurisdicao_vigencia CHECK (vigencia_fim IS NULL OR vigencia_fim > vigencia_inicio),
    CONSTRAINT ex_jurisdicao_sem_sobreposicao EXCLUDE USING gist (
        municipio_ibge WITH =,
        tipo_justica WITH =,
        daterange(vigencia_inicio, vigencia_fim, '[)') WITH &&
    )
);

CREATE INDEX IF NOT EXISTS idx_jurisdicao_lookup
    ON tb_jurisdicao_territorial (municipio_ibge, tipo_justica, vigencia_inicio DESC);
