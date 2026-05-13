CREATE TABLE IF NOT EXISTS tb_documento_triage_signal (
    id UUID PRIMARY KEY,
    doc_id UUID NOT NULL,
    processo_id BIGINT NOT NULL,
    work_item_id BIGINT,
    urgency_level VARCHAR(20) NOT NULL,
    score INTEGER NOT NULL,
    tags_json TEXT NOT NULL,
    keywords TEXT,
    rationale VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_triage_doc_id ON tb_documento_triage_signal(doc_id);
CREATE INDEX IF NOT EXISTS idx_triage_processo_id ON tb_documento_triage_signal(processo_id);
CREATE INDEX IF NOT EXISTS idx_triage_work_item_id ON tb_documento_triage_signal(work_item_id);
CREATE INDEX IF NOT EXISTS idx_triage_score ON tb_documento_triage_signal(score DESC);

ALTER TABLE tb_documento_processual
    ADD COLUMN IF NOT EXISTS sha384 VARCHAR(96);

ALTER TABLE tb_documento_processual
    ADD COLUMN IF NOT EXISTS storage_backend VARCHAR(40);

ALTER TABLE tb_documento_processual
    ADD COLUMN IF NOT EXISTS storage_uri VARCHAR(600);

ALTER TABLE tb_documento_processual
    ADD COLUMN IF NOT EXISTS externalized_at TIMESTAMPTZ;
