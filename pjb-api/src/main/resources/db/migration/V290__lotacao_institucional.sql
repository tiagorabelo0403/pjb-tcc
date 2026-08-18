CREATE TABLE tb_lotacao_institucional (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT      NOT NULL REFERENCES tb_usuario(id),
    unidade_id      BIGINT      NOT NULL REFERENCES tb_unidade_institucional(id),
    inicio          DATE        NOT NULL,
    fim             DATE,
    papel_na_unidade VARCHAR(60)
);

CREATE INDEX idx_tb_lotacao_usuario ON tb_lotacao_institucional (usuario_id);
CREATE INDEX idx_tb_lotacao_unidade ON tb_lotacao_institucional (unidade_id);

CREATE UNIQUE INDEX uq_lotacao_ativa ON tb_lotacao_institucional (usuario_id, unidade_id) WHERE fim IS NULL;
