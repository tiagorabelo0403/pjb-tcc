ALTER TABLE tb_inquerito_policial_digital
    ADD COLUMN IF NOT EXISTS unidade_apuracao_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_inquerito_unidade_apuracao') THEN
        ALTER TABLE tb_inquerito_policial_digital
            ADD CONSTRAINT fk_inquerito_unidade_apuracao
                FOREIGN KEY (unidade_apuracao_id) REFERENCES tb_unidade_institucional(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_inquerito_unidade_apuracao_status
    ON tb_inquerito_policial_digital (unidade_apuracao_id, status, updated_at);
