-- Serviços Judiciais (PJB 2026) - Publicações Oficiais + Sessão Plenária

CREATE TABLE IF NOT EXISTS tb_publicacao_oficial (
    id BIGSERIAL PRIMARY KEY,
    processo_id BIGINT,
    fonte VARCHAR(60) NOT NULL,
    data_publicacao DATE,
    titulo VARCHAR(260),
    resumo TEXT,
    conteudo TEXT,
    url_referencia VARCHAR(800),
    hash_sha256 VARCHAR(64),
    lida BOOLEAN DEFAULT FALSE,
    criado_em TIMESTAMP
);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_publicacao_processo') THEN
        ALTER TABLE tb_publicacao_oficial
            ADD CONSTRAINT fk_publicacao_processo
            FOREIGN KEY (processo_id) REFERENCES tb_processo(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_publicacao_processo ON tb_publicacao_oficial(processo_id);
CREATE INDEX IF NOT EXISTS idx_publicacao_data ON tb_publicacao_oficial(data_publicacao);

CREATE TABLE IF NOT EXISTS tb_sessao_plenaria (
    id BIGSERIAL PRIMARY KEY,
    fonte VARCHAR(30) NOT NULL,
    orgao VARCHAR(120),
    modalidade VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    data_hora TIMESTAMP NOT NULL,
    local VARCHAR(260),
    observacoes TEXT,
    criado_em TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sessao_data ON tb_sessao_plenaria(data_hora);

CREATE TABLE IF NOT EXISTS tb_sessao_plenaria_item (
    id BIGSERIAL PRIMARY KEY,
    sessao_id BIGINT NOT NULL,
    ordem INT NOT NULL,
    tipo VARCHAR(60) NOT NULL,
    processo_id BIGINT,
    tema VARCHAR(400),
    relator VARCHAR(220),
    status VARCHAR(40) NOT NULL,
    resultado TEXT,
    criado_em TIMESTAMP
);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sessao_item_sessao') THEN
        ALTER TABLE tb_sessao_plenaria_item
            ADD CONSTRAINT fk_sessao_item_sessao
            FOREIGN KEY (sessao_id) REFERENCES tb_sessao_plenaria(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sessao_item_processo') THEN
        ALTER TABLE tb_sessao_plenaria_item
            ADD CONSTRAINT fk_sessao_item_processo
            FOREIGN KEY (processo_id) REFERENCES tb_processo(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_sessao_item_sessao ON tb_sessao_plenaria_item(sessao_id);
CREATE INDEX IF NOT EXISTS idx_sessao_item_processo ON tb_sessao_plenaria_item(processo_id);

CREATE TABLE IF NOT EXISTS tb_sessao_voto (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL,
    votante_id BIGINT,
    voto VARCHAR(60) NOT NULL,
    fundamentacao TEXT,
    criado_em TIMESTAMP
);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sessao_voto_item') THEN
        ALTER TABLE tb_sessao_voto
            ADD CONSTRAINT fk_sessao_voto_item
            FOREIGN KEY (item_id) REFERENCES tb_sessao_plenaria_item(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_sessao_voto_item ON tb_sessao_voto(item_id);
