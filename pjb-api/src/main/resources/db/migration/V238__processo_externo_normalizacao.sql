-- V238: external process import normalization log

CREATE TABLE IF NOT EXISTS pjb_processo_externo_carga (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero_cnj          VARCHAR(25)  NOT NULL,
    sistema_origem      VARCHAR(40)  NOT NULL,
    status              VARCHAR(40)  NOT NULL DEFAULT 'PENDENTE',
    rito_detectado      VARCHAR(80),
    payload_hash        CHAR(64),
    tentativas          INTEGER      NOT NULL DEFAULT 0,
    ultima_tentativa    TIMESTAMPTZ,
    erro_descricao      TEXT,
    processo_id         UUID,
    iniciado_em         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    concluido_em        TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_externo_numero_sistema ON pjb_processo_externo_carga (numero_cnj, sistema_origem);
CREATE INDEX IF NOT EXISTS idx_externo_status              ON pjb_processo_externo_carga (status);
CREATE INDEX IF NOT EXISTS idx_externo_processo            ON pjb_processo_externo_carga (processo_id) WHERE processo_id IS NOT NULL;
