-- V241: gigs activity and execution tracking

CREATE TABLE IF NOT EXISTS pjb_gigs_activity (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processo_id         UUID         NOT NULL,
    tipo                VARCHAR(60)  NOT NULL,
    descricao           TEXT,
    executado_por       VARCHAR(120),
    visibilidade        VARCHAR(40)  NOT NULL DEFAULT 'PUBLICA',
    sigilo_nivel        INTEGER      NOT NULL DEFAULT 0,
    iniciado_em         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    concluido_em        TIMESTAMPTZ,
    status              VARCHAR(40)  NOT NULL DEFAULT 'PENDENTE'
);

CREATE INDEX IF NOT EXISTS idx_gigs_processo ON pjb_gigs_activity (processo_id);
CREATE INDEX IF NOT EXISTS idx_gigs_tipo     ON pjb_gigs_activity (tipo);
CREATE INDEX IF NOT EXISTS idx_gigs_status   ON pjb_gigs_activity (status);

CREATE TABLE IF NOT EXISTS pjb_gigs_comment (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    activity_id         UUID         NOT NULL REFERENCES pjb_gigs_activity (id),
    autor               VARCHAR(120) NOT NULL,
    texto               TEXT         NOT NULL,
    criado_em           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_gigs_comment_activity ON pjb_gigs_comment (activity_id);
