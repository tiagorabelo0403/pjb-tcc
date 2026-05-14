-- V239: celeridade processual analytics snapshots

CREATE TABLE IF NOT EXISTS pjb_celeridade_snapshot (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vara_id             VARCHAR(60)  NOT NULL,
    rito                VARCHAR(80),
    periodo_inicio      DATE         NOT NULL,
    periodo_fim         DATE         NOT NULL,
    tempo_medio_dias    NUMERIC(10,2),
    tempo_mediano_dias  NUMERIC(10,2),
    processos_analisados INTEGER     NOT NULL DEFAULT 0,
    gargalo_principal   VARCHAR(120),
    retrabalho_taxa     NUMERIC(5,4) DEFAULT 0,
    calculado_em        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_celeridade_vara   ON pjb_celeridade_snapshot (vara_id);
CREATE INDEX IF NOT EXISTS idx_celeridade_rito   ON pjb_celeridade_snapshot (rito) WHERE rito IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_celeridade_periodo ON pjb_celeridade_snapshot (periodo_fim DESC);
