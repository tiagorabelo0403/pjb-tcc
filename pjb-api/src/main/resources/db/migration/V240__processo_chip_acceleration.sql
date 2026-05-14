-- V240: processo chip detection and acceleration tracking (Round 28AH)

CREATE TABLE IF NOT EXISTS pjb_processo_chip (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processo_id         UUID         NOT NULL,
    chip_tipo           VARCHAR(60)  NOT NULL,
    score_prioridade    INTEGER      NOT NULL DEFAULT 0,
    detectado_em        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expirado_em         TIMESTAMPTZ,
    ativo               BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_chip_processo ON pjb_processo_chip (processo_id);
CREATE INDEX IF NOT EXISTS idx_chip_tipo     ON pjb_processo_chip (chip_tipo);
CREATE INDEX IF NOT EXISTS idx_chip_ativo    ON pjb_processo_chip (processo_id, chip_tipo) WHERE ativo = TRUE;
