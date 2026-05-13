CREATE TABLE IF NOT EXISTS tb_upload_batch (
    id UUID PRIMARY KEY,
    processo_id BIGINT NOT NULL,
    expected_count INTEGER,
    status VARCHAR(30) NOT NULL,
    created_by BIGINT,
    created_at TIMESTAMPTZ NOT NULL,
    finalized_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS tb_upload_item (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL,
    seq INTEGER NOT NULL,
    nome_original VARCHAR(255),
    content_type VARCHAR(120),
    tamanho_bytes BIGINT,
    storage_key VARCHAR(900),
    storage_backend VARCHAR(40),
    storage_uri VARCHAR(900),
    hash_sha256 VARCHAR(64),
    hash_sha384 VARCHAR(96),
    token_hash VARCHAR(96),
    token_expires_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL,
    uploaded_at TIMESTAMPTZ
);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_upload_item_batch') THEN
        ALTER TABLE tb_upload_item
            ADD CONSTRAINT fk_upload_item_batch
            FOREIGN KEY (batch_id) REFERENCES tb_upload_batch(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_upload_item_batch_seq ON tb_upload_item(batch_id, seq);
CREATE INDEX IF NOT EXISTS idx_upload_item_batch ON tb_upload_item(batch_id);
CREATE INDEX IF NOT EXISTS idx_upload_item_tokenhash ON tb_upload_item(token_hash);
CREATE INDEX IF NOT EXISTS idx_upload_batch_processo ON tb_upload_batch(processo_id);
