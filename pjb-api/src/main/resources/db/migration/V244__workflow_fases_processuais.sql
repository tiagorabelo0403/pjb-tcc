-- V244: workflow de fases processuais e atos obrigatórios (Round 28AI)

CREATE TABLE IF NOT EXISTS pjb_fase_transicao_log (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processo_id         UUID         NOT NULL,
    fase_anterior       VARCHAR(60)  NOT NULL,
    fase_atual          VARCHAR(60)  NOT NULL,
    efetivada           BOOLEAN      NOT NULL DEFAULT FALSE,
    bloqueios           TEXT[],
    solicitante_id      VARCHAR(120),
    ocorrido_em         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_fase_transicao_processo ON pjb_fase_transicao_log (processo_id);
CREATE INDEX IF NOT EXISTS idx_fase_transicao_fase_atual ON pjb_fase_transicao_log (fase_atual);

CREATE TABLE IF NOT EXISTS pjb_ato_processual_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processo_id     UUID         NOT NULL,
    codigo_ato      VARCHAR(80)  NOT NULL,
    fase            VARCHAR(60),
    e_jurisdicional BOOLEAN      NOT NULL DEFAULT FALSE,
    executado_por   VARCHAR(120),
    registrado_em   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ato_processual_processo ON pjb_ato_processual_log (processo_id);
CREATE INDEX IF NOT EXISTS idx_ato_processual_codigo   ON pjb_ato_processual_log (codigo_ato);
