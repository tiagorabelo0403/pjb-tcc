ALTER TABLE tb_jurisdicao_territorial
    DROP COLUMN unidade_codigo;

CREATE TABLE IF NOT EXISTS tb_jurisdicao_territorial_unidade (
    jurisdicao_territorial_id BIGINT NOT NULL REFERENCES tb_jurisdicao_territorial(id) ON DELETE CASCADE,
    unidade_codigo VARCHAR(80) NOT NULL,
    PRIMARY KEY (jurisdicao_territorial_id, unidade_codigo)
);
