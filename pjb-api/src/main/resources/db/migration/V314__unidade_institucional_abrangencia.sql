CREATE TABLE tb_unidade_institucional_abrangencia (
    id                          BIGSERIAL PRIMARY KEY,
    unidade_institucional_id    BIGINT NOT NULL REFERENCES tb_unidade_institucional(id),
    comarca_atendida            VARCHAR(120) NOT NULL,
    UNIQUE (unidade_institucional_id, comarca_atendida)
);

CREATE INDEX idx_tb_unid_abrangencia_comarca ON tb_unidade_institucional_abrangencia(comarca_atendida);
