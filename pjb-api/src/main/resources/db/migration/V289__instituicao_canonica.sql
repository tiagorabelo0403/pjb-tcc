CREATE TABLE tb_instituicao (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID         NOT NULL UNIQUE,
    tipo            VARCHAR(60)  NOT NULL,
    nome            VARCHAR(255) NOT NULL,
    sigla           VARCHAR(30),
    identificador_oficial VARCHAR(20),
    esfera          VARCHAR(20),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ATIVA',
    criado_em       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tb_inst_tipo   ON tb_instituicao (tipo);
CREATE INDEX idx_tb_inst_status ON tb_instituicao (status);

CREATE TABLE tb_unidade_institucional (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID         NOT NULL UNIQUE,
    instituicao_id  BIGINT       NOT NULL REFERENCES tb_instituicao(id),
    parent_id       BIGINT       REFERENCES tb_unidade_institucional(id),
    nome            VARCHAR(255) NOT NULL,
    tipo            VARCHAR(60)  NOT NULL,
    status_unidade  VARCHAR(20)  NOT NULL DEFAULT 'ATIVA',
    comarca         VARCHAR(120),
    uf              CHAR(2),
    criado_em       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tb_unid_instituicao  ON tb_unidade_institucional (instituicao_id);
CREATE INDEX idx_tb_unid_parent       ON tb_unidade_institucional (parent_id);
CREATE INDEX idx_tb_unid_tipo_comarca ON tb_unidade_institucional (tipo, comarca);
