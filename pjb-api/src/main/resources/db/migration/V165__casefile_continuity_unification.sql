ALTER TABLE tb_case_proceeding
    ADD COLUMN IF NOT EXISTS continuity_track VARCHAR(30) NOT NULL DEFAULT 'CONHECIMENTO',
    ADD COLUMN IF NOT EXISTS proceeding_role VARCHAR(30) NOT NULL DEFAULT 'ROOT',
    ADD COLUMN IF NOT EXISTS parent_proceeding_key VARCHAR(64),
    ADD COLUMN IF NOT EXISTS source_fase_processual VARCHAR(60),
    ADD COLUMN IF NOT EXISTS source_status_processo VARCHAR(60),
    ADD COLUMN IF NOT EXISTS last_sync_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS ix_case_proceeding_linked_processo ON tb_case_proceeding(linked_processo_id);
CREATE INDEX IF NOT EXISTS ix_case_proceeding_parent_key ON tb_case_proceeding(parent_proceeding_key);
CREATE INDEX IF NOT EXISTS ix_case_proceeding_track_role ON tb_case_proceeding(continuity_track, proceeding_role);

UPDATE tb_case_proceeding
SET continuity_track = CASE
    WHEN shadow THEN 'RECURSAL'
    ELSE 'CONHECIMENTO'
END,
    proceeding_role = CASE
    WHEN shadow THEN 'RECURSAL'
    ELSE 'ROOT'
END,
    last_sync_at = COALESCE(last_sync_at, updated_at, created_at)
WHERE continuity_track IS NULL
   OR proceeding_role IS NULL
   OR last_sync_at IS NULL;
