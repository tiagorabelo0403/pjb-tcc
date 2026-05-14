-- V233: secretariat signature queue and batch signature control

CREATE TABLE IF NOT EXISTS pjb_assinatura_pendente (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processo_id         UUID         NOT NULL,
    documento_id        UUID         NOT NULL,
    tipo_documento      VARCHAR(80)  NOT NULL,
    tags                TEXT[]       NOT NULL DEFAULT '{}',
    sha256_esperado     CHAR(64),
    cert_valido         BOOLEAN      NOT NULL DEFAULT FALSE,
    renderizacao_ok     BOOLEAN      NOT NULL DEFAULT FALSE,
    bloqueante          BOOLEAN      NOT NULL DEFAULT FALSE,
    criado_em           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    assinado_em         TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_assinatura_processo  ON pjb_assinatura_pendente (processo_id);
CREATE INDEX IF NOT EXISTS idx_assinatura_pendente  ON pjb_assinatura_pendente (assinado_em) WHERE assinado_em IS NULL;

CREATE TABLE IF NOT EXISTS pjb_assinatura_lote (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    itens               UUID[]       NOT NULL DEFAULT '{}',
    executado_por       VARCHAR(120),
    iniciado_em         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    concluido_em        TIMESTAMPTZ,
    status              VARCHAR(40)  NOT NULL DEFAULT 'PENDENTE'
);
