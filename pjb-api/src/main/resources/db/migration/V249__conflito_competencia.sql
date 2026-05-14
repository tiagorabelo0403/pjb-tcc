-- V249: conflito de competência — detecção e log (Round 28AI)

CREATE TABLE IF NOT EXISTS pjb_conflito_competencia (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processo_id         UUID         NOT NULL,
    tipo_conflito       VARCHAR(40)  NOT NULL,
    orgao_atual         VARCHAR(120) NOT NULL,
    orgao_alternativo   VARCHAR(120),
    motivo              TEXT         NOT NULL,
    requer_decisao      BOOLEAN      NOT NULL DEFAULT TRUE,
    resolvido           BOOLEAN      NOT NULL DEFAULT FALSE,
    resolucao           TEXT,
    detectado_em        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resolvido_em        TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_conflito_processo ON pjb_conflito_competencia (processo_id);
CREATE INDEX IF NOT EXISTS idx_conflito_resolvido ON pjb_conflito_competencia (resolvido)
    WHERE resolvido = FALSE;
