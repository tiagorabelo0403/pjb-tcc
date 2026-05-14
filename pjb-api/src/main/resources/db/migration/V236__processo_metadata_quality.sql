-- V236: processo metadata quality scoring and completeness snapshots

CREATE TABLE IF NOT EXISTS pjb_metadata_quality_snapshot (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processo_id         UUID         NOT NULL,
    score               INTEGER      NOT NULL CHECK (score BETWEEN 0 AND 100),
    nivel               VARCHAR(20)  NOT NULL,
    issues              JSONB        NOT NULL DEFAULT '[]',
    calculado_em        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_metadata_quality_processo ON pjb_metadata_quality_snapshot (processo_id);
CREATE INDEX IF NOT EXISTS idx_metadata_quality_nivel    ON pjb_metadata_quality_snapshot (nivel);
CREATE INDEX IF NOT EXISTS idx_metadata_quality_score    ON pjb_metadata_quality_snapshot (score);
