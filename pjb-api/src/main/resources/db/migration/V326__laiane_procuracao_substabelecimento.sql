ALTER TABLE tb_laiane_procuracao
    ADD COLUMN IF NOT EXISTS substabelecido_de_id BIGINT,
    ADD COLUMN IF NOT EXISTS com_reserva_de_poderes BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE tb_laiane_procuracao
    ADD CONSTRAINT fk_laiane_procuracao_substabelecido_de
        FOREIGN KEY (substabelecido_de_id) REFERENCES tb_laiane_procuracao(id);

CREATE INDEX IF NOT EXISTS idx_laiane_procuracao_substabelecido_de
    ON tb_laiane_procuracao(substabelecido_de_id);
