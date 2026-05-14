-- V234: secretariat expediente panel and ciência tracking

CREATE TABLE IF NOT EXISTS pjb_expediente_painel (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processo_id         UUID         NOT NULL,
    tipo_expediente     VARCHAR(80)  NOT NULL,
    situacao            VARCHAR(40)  NOT NULL DEFAULT 'AGUARDANDO_CIENCIA',
    destaque            BOOLEAN      NOT NULL DEFAULT FALSE,
    prazo_fatal         TIMESTAMPTZ,
    ciencia_em          TIMESTAMPTZ,
    ciencia_por         VARCHAR(120),
    criado_em           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_expediente_processo   ON pjb_expediente_painel (processo_id);
CREATE INDEX IF NOT EXISTS idx_expediente_situacao   ON pjb_expediente_painel (situacao);
CREATE INDEX IF NOT EXISTS idx_expediente_prazo      ON pjb_expediente_painel (prazo_fatal) WHERE prazo_fatal IS NOT NULL AND ciencia_em IS NULL;
