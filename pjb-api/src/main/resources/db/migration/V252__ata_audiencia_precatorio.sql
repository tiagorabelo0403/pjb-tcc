-- V252: ata de audiência, precatório/RPV e extinção sem mérito (Round 28AI)

CREATE TABLE IF NOT EXISTS pjb_ata_audiencia (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processo_id         UUID         NOT NULL,
    audiencia_id        UUID         NOT NULL,
    tipo_audiencia      VARCHAR(80)  NOT NULL,
    data_audiencia      DATE         NOT NULL,
    magistrado          VARCHAR(255),
    servidor_presente   VARCHAR(255),
    resultado           VARCHAR(120),
    texto_ata           TEXT,
    determinacoes       TEXT[],
    assinada            BOOLEAN      NOT NULL DEFAULT FALSE,
    criado_em           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ata_processo   ON pjb_ata_audiencia (processo_id);
CREATE INDEX IF NOT EXISTS idx_ata_audiencia  ON pjb_ata_audiencia (audiencia_id);

CREATE TABLE IF NOT EXISTS pjb_precatorio_rpv (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processo_id         UUID          NOT NULL,
    tipo                VARCHAR(20)   NOT NULL CHECK (tipo IN ('RPV','PRECATORIO')),
    valor               NUMERIC(18,2) NOT NULL,
    natureza_alimentar  BOOLEAN       NOT NULL DEFAULT FALSE,
    tipo_fazenda        VARCHAR(20)   NOT NULL,
    transito_em_julgado DATE,
    status              VARCHAR(40)   NOT NULL DEFAULT 'PENDENTE',
    registrado_em       TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_precatorio_processo ON pjb_precatorio_rpv (processo_id);
CREATE INDEX IF NOT EXISTS idx_precatorio_status   ON pjb_precatorio_rpv (status);
