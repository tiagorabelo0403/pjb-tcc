ALTER TABLE download_events
    ADD COLUMN IF NOT EXISTS processo_id BIGINT,
    ADD COLUMN IF NOT EXISTS documento_id VARCHAR(36);

CREATE INDEX IF NOT EXISTS idx_de_user_processo_time ON download_events(usuario_id, processo_id, created_at);
CREATE INDEX IF NOT EXISTS idx_de_user_documento_time ON download_events(usuario_id, documento_id, created_at);
