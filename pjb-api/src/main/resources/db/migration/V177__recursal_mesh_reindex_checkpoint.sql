CREATE TABLE IF NOT EXISTS tb_recursal_mesh_reindex_checkpoint (
    checkpoint_key VARCHAR(120) PRIMARY KEY,
    index_name VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL,
    lock_owner VARCHAR(120),
    last_processed_updated_at TIMESTAMPTZ,
    last_processed_recurso_id VARCHAR(160),
    processed_count INTEGER NOT NULL DEFAULT 0,
    indexed_count INTEGER NOT NULL DEFAULT 0,
    skipped_count INTEGER NOT NULL DEFAULT 0,
    batch_count INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_recursal_mesh_reindex_checkpoint_status
    ON tb_recursal_mesh_reindex_checkpoint(status, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_recursal_mesh_reindex_checkpoint_index
    ON tb_recursal_mesh_reindex_checkpoint(index_name, updated_at DESC);
