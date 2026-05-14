-- V235: autuação rectification audit trail (Round 28AH)

CREATE TABLE IF NOT EXISTS pjb_retificacao_autuacao (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processo_id         UUID         NOT NULL,
    status              VARCHAR(40)  NOT NULL DEFAULT 'PENDENTE',
    campos_alterados    TEXT[]       NOT NULL DEFAULT '{}',
    valor_anterior      JSONB,
    valor_novo          JSONB,
    motivo              TEXT,
    solicitado_por      VARCHAR(120) NOT NULL,
    solicitado_em       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    aprovado_por        VARCHAR(120),
    aprovado_em         TIMESTAMPTZ,
    aplicado_em         TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_retificacao_processo ON pjb_retificacao_autuacao (processo_id);
CREATE INDEX IF NOT EXISTS idx_retificacao_status   ON pjb_retificacao_autuacao (status);
