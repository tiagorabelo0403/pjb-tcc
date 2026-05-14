-- V247: custas e multas processuais (Round 28AI)

CREATE TABLE IF NOT EXISTS pjb_custas_processual (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processo_id         UUID         NOT NULL,
    parte_id            VARCHAR(120),
    tipo                VARCHAR(60)  NOT NULL,
    valor_base          NUMERIC(18,2) NOT NULL DEFAULT 0,
    percentual          NUMERIC(7,4)  NOT NULL DEFAULT 0,
    valor_final         NUMERIC(18,2) NOT NULL DEFAULT 0,
    isento              BOOLEAN       NOT NULL DEFAULT FALSE,
    fundamentacao       TEXT,
    quitado             BOOLEAN       NOT NULL DEFAULT FALSE,
    quitado_em          TIMESTAMPTZ,
    criado_em           TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_custas_processo ON pjb_custas_processual (processo_id);
CREATE INDEX IF NOT EXISTS idx_custas_tipo     ON pjb_custas_processual (tipo);
CREATE INDEX IF NOT EXISTS idx_custas_quitado  ON pjb_custas_processual (quitado, processo_id);
