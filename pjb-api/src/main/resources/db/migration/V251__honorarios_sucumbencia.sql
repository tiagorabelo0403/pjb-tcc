-- V251: honorários de sucumbência e tutela urgência (Round 28AI)

CREATE TABLE IF NOT EXISTS pjb_honorarios_sucumbencia (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processo_id         UUID          NOT NULL,
    parte_sucumbente    VARCHAR(120),
    valor_condenacao    NUMERIC(18,2) NOT NULL DEFAULT 0,
    percentual_aplicado NUMERIC(7,4)  NOT NULL DEFAULT 0,
    valor_honorarios    NUMERIC(18,2) NOT NULL DEFAULT 0,
    fazenda_publica     BOOLEAN       NOT NULL DEFAULT FALSE,
    fundamentacao       TEXT,
    calculado_em        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    homologado_em       TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_honorarios_processo ON pjb_honorarios_sucumbencia (processo_id);

CREATE TABLE IF NOT EXISTS pjb_tutela_urgencia (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processo_id         UUID         NOT NULL,
    tipo                VARCHAR(40)  NOT NULL,
    pedido_formulado    BOOLEAN      NOT NULL DEFAULT FALSE,
    probabilidade_direito BOOLEAN    NOT NULL DEFAULT FALSE,
    perigo_demora       BOOLEAN      NOT NULL DEFAULT FALSE,
    reversivel          BOOLEAN      NOT NULL DEFAULT TRUE,
    apta_apreciacao     BOOLEAN      NOT NULL DEFAULT FALSE,
    registrado_em       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_tutela_processo ON pjb_tutela_urgencia (processo_id);
