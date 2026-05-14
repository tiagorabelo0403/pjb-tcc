-- V242: process mining materialized view support tables (Round 28AH)

CREATE TABLE IF NOT EXISTS pjb_mining_event_log (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processo_id         UUID         NOT NULL,
    tipo_ato            VARCHAR(80)  NOT NULL,
    sistema_origem      VARCHAR(40),
    rito                VARCHAR(80),
    fase                VARCHAR(80),
    categoria_documento VARCHAR(80),
    canal_comunicacao   VARCHAR(60),
    duracao_horas       NUMERIC(10,4) NOT NULL DEFAULT 0,
    registrado_em       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_mining_processo    ON pjb_mining_event_log (processo_id);
CREATE INDEX IF NOT EXISTS idx_mining_tipo_ato    ON pjb_mining_event_log (tipo_ato);
CREATE INDEX IF NOT EXISTS idx_mining_rito        ON pjb_mining_event_log (rito) WHERE rito IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_mining_fase        ON pjb_mining_event_log (fase) WHERE fase IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_mining_registrado  ON pjb_mining_event_log (registrado_em DESC);

CREATE TABLE IF NOT EXISTS pjb_mining_snapshot (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dimensao            VARCHAR(40)  NOT NULL,
    chave               VARCHAR(120) NOT NULL,
    quantidade          BIGINT       NOT NULL DEFAULT 0,
    tempo_medio_horas   NUMERIC(10,4) NOT NULL DEFAULT 0,
    impacto_relativo    NUMERIC(7,6) NOT NULL DEFAULT 0,
    atualizado_em       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_mining_snapshot_dimensao ON pjb_mining_snapshot (dimensao, chave);
