CREATE TABLE tb_tribunal (
    id BIGSERIAL PRIMARY KEY,
    sigla VARCHAR(20) NOT NULL,
    nome VARCHAR(200) NOT NULL,
    tipo_justica VARCHAR(30) NOT NULL,
    grau VARCHAR(20) NOT NULL,
    uf_sede VARCHAR(2)
);

CREATE UNIQUE INDEX uk_tribunal_sigla ON tb_tribunal (sigla);

CREATE TABLE tb_comarca (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    uf VARCHAR(2) NOT NULL,
    municipio_sede_ibge VARCHAR(7) NOT NULL,
    nome_foro VARCHAR(200),
    tribunal_id BIGINT REFERENCES tb_tribunal(id)
);

CREATE INDEX idx_comarca_ibge ON tb_comarca (municipio_sede_ibge);
CREATE INDEX idx_comarca_nome_uf ON tb_comarca (nome, uf);
