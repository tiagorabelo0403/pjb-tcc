ALTER TABLE tb_secretariat_queue_item
    ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;
