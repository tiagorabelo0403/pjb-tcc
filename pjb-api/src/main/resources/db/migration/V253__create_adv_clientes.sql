CREATE TABLE IF NOT EXISTS adv_clientes (
    id                   BIGSERIAL PRIMARY KEY,
    advogado_id          BIGINT       NOT NULL,
    equipe_id            BIGINT,
    nome                 VARCHAR(150) NOT NULL,
    nome_normalizado     VARCHAR(150) NOT NULL,
    cpf_criptografado    VARCHAR(1000),
    cpf_hash             VARCHAR(64),
    email_criptografado  VARCHAR(1000),
    status               VARCHAR(20)  NOT NULL DEFAULT 'ATIVO',
    telefone             VARCHAR(20),
    endereco             VARCHAR(255),
    observacoes          TEXT,
    data_criacao         TIMESTAMP    NOT NULL,
    data_atualizacao     TIMESTAMP    NOT NULL,
    versao               BIGINT,
    nivel_confiabilidade DOUBLE PRECISION DEFAULT 1.0,
    ultima_analise_ia    TIMESTAMP,
    CONSTRAINT fk_adv_cliente_advogado FOREIGN KEY (advogado_id) REFERENCES tb_usuario(id),
    CONSTRAINT fk_adv_cliente_equipe   FOREIGN KEY (equipe_id)   REFERENCES equipes(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_cliente_advogado_cpf_hash
    ON adv_clientes (advogado_id, cpf_hash);

CREATE INDEX IF NOT EXISTS idx_cliente_cpf_hash
    ON adv_clientes (cpf_hash);

CREATE INDEX IF NOT EXISTS idx_cliente_advogado_status
    ON adv_clientes (advogado_id, status);
