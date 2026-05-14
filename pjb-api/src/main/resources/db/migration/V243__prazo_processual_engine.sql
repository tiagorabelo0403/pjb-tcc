-- V243: prazo processual engine — calendário, vencimentos e certidões (Round 28AI)

CREATE TABLE IF NOT EXISTS pjb_prazo_processual (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processo_id         UUID         NOT NULL,
    tipo                VARCHAR(40)  NOT NULL,
    situacao            VARCHAR(40)  NOT NULL DEFAULT 'ABERTO',
    descricao           VARCHAR(255) NOT NULL,
    data_inicio         DATE         NOT NULL,
    data_vencimento     DATE         NOT NULL,
    dias_uteis_total    INTEGER      NOT NULL DEFAULT 0,
    fazenda_publica     BOOLEAN      NOT NULL DEFAULT FALSE,
    ministerio_publico  BOOLEAN      NOT NULL DEFAULT FALSE,
    comarca_id          VARCHAR(60),
    uf                  CHAR(2),
    criado_em           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_prazo_processo    ON pjb_prazo_processual (processo_id);
CREATE INDEX IF NOT EXISTS idx_prazo_situacao    ON pjb_prazo_processual (situacao);
CREATE INDEX IF NOT EXISTS idx_prazo_vencimento  ON pjb_prazo_processual (data_vencimento)
    WHERE situacao IN ('ABERTO','PRORROGADO');

CREATE TABLE IF NOT EXISTS pjb_prazo_evento (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prazo_id    UUID         NOT NULL REFERENCES pjb_prazo_processual (id),
    tipo_evento VARCHAR(40)  NOT NULL,
    data_evento DATE         NOT NULL,
    descricao   TEXT,
    registrado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_prazo_evento_prazo ON pjb_prazo_evento (prazo_id);
