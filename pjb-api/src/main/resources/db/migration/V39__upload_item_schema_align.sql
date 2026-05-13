DROP INDEX IF EXISTS uq_upload_item_batch_seq;

ALTER TABLE tb_upload_item
    DROP COLUMN IF EXISTS seq;

ALTER TABLE tb_upload_item
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE tb_upload_item
    ADD COLUMN IF NOT EXISTS edge_attestation_json TEXT;

ALTER TABLE tb_upload_item
    ADD COLUMN IF NOT EXISTS linked_document_id UUID;

CREATE INDEX IF NOT EXISTS ix_upload_item_batch_status_created
    ON tb_upload_item(batch_id, status, created_at);
