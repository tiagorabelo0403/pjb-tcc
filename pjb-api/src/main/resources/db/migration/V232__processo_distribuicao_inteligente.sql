-- V232: intelligent distribution and workload balancing tables (Round 28AH)

CREATE TABLE IF NOT EXISTS pjb_distribuicao_snapshot (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processo_id         UUID         NOT NULL,
    vara_id             VARCHAR(60)  NOT NULL,
    servidor_id         VARCHAR(80),
    tipo_atividade      VARCHAR(80),
    score_carga         INTEGER      NOT NULL DEFAULT 0,
    justificativa       TEXT,
    sugerido_em         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    confirmado_em       TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_distribuicao_processo ON pjb_distribuicao_snapshot (processo_id);
CREATE INDEX IF NOT EXISTS idx_distribuicao_vara     ON pjb_distribuicao_snapshot (vara_id);
CREATE INDEX IF NOT EXISTS idx_distribuicao_servidor ON pjb_distribuicao_snapshot (servidor_id) WHERE servidor_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS pjb_responsavel_carga (
    servidor_id         VARCHAR(80)  NOT NULL,
    nome                VARCHAR(255) NOT NULL,
    tarefas_ativas      INTEGER      NOT NULL DEFAULT 0,
    atualizado_em       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (servidor_id)
);
