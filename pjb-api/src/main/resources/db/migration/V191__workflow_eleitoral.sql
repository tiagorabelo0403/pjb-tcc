CREATE TABLE IF NOT EXISTS pjb_calendario_eleitoral (
 id BIGSERIAL PRIMARY KEY,
 ano_eleitoral INT NOT NULL,
 tipo_eleicao VARCHAR(32) NOT NULL,
 fase VARCHAR(64) NOT NULL,
 data_inicio DATE NOT NULL,
 data_fim DATE NOT NULL,
 descricao TEXT,
 zona_eleitoral VARCHAR(16),
 uf VARCHAR(2),
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 CONSTRAINT uk_cal_eleitoral UNIQUE (ano_eleitoral, tipo_eleicao, fase, uf)
);
CREATE INDEX IF NOT EXISTS idx_cal_eleitoral_fase ON pjb_calendario_eleitoral (data_fim, data_inicio);
CREATE TABLE IF NOT EXISTS pjb_processo_zona_eleitoral (
 processo_id BIGINT PRIMARY KEY REFERENCES tb_processo(id) ON DELETE CASCADE,
 zona_eleitoral VARCHAR(16),
 municipio VARCHAR(120),
 uf VARCHAR(2),
 cartorio_codigo VARCHAR(16),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE IF NOT EXISTS pjb_feito_eleitoral_especial (
 id BIGSERIAL PRIMARY KEY,
 processo_id BIGINT NOT NULL REFERENCES tb_processo(id) ON DELETE RESTRICT,
 tipo_feito VARCHAR(64) NOT NULL,
 numero_candidato VARCHAR(16),
 partido_sigla VARCHAR(16),
 cargo VARCHAR(64),
 ano_eleitoral INT,
 status_eleitoral VARCHAR(32) NOT NULL DEFAULT 'EM_ANDAMENTO',
 diplomado_em DATE,
 extinto_em TIMESTAMPTZ,
 motivo_extincao TEXT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_feito_eleitoral_processo ON pjb_feito_eleitoral_especial (processo_id);
CREATE INDEX IF NOT EXISTS idx_feito_eleitoral_partido ON pjb_feito_eleitoral_especial (partido_sigla, ano_eleitoral);
