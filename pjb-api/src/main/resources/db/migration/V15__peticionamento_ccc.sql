-- Serviços Judiciais (PJB 2026) - Peticionamento Intermediário + CCC

CREATE TABLE IF NOT EXISTS tb_cadastro_central_pessoa (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(10) NOT NULL,
    cpf_cnpj VARCHAR(20) NOT NULL UNIQUE,
    nome_razao VARCHAR(260) NOT NULL,
    dados_json TEXT,
    ativo BOOLEAN DEFAULT TRUE,
    criado_em TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ccc_nome ON tb_cadastro_central_pessoa(nome_razao);

CREATE TABLE IF NOT EXISTS tb_peticionamento_intermediario (
    id UUID PRIMARY KEY,
    processo_id BIGINT,
    numero_processo VARCHAR(30),
    classificacao VARCHAR(120) NOT NULL,
    categoria VARCHAR(120),
    pedido_urgencia BOOLEAN DEFAULT FALSE,
    solicitante_id BIGINT,
    solicitante_nome VARCHAR(260),
    solicitante_cpf_cnpj VARCHAR(20),
    sociedade_nome VARCHAR(260),
    sociedade_cnpj VARCHAR(20),
    sociedade_oab VARCHAR(40),
    status VARCHAR(40) NOT NULL,
    protocolo VARCHAR(80),
    email_contato VARCHAR(260),
    observacoes TEXT,
    criado_em TIMESTAMP,
    atualizado_em TIMESTAMP
);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pet_inter_processo') THEN
        ALTER TABLE tb_peticionamento_intermediario
            ADD CONSTRAINT fk_pet_inter_processo
            FOREIGN KEY (processo_id) REFERENCES tb_processo(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_pet_inter_processo ON tb_peticionamento_intermediario(processo_id);
CREATE INDEX IF NOT EXISTS idx_pet_inter_status ON tb_peticionamento_intermediario(status);

CREATE TABLE IF NOT EXISTS tb_peticionamento_parte (
    id BIGSERIAL PRIMARY KEY,
    peticionamento_id UUID NOT NULL,
    papel VARCHAR(60) NOT NULL,
    nome VARCHAR(260) NOT NULL,
    cpf_cnpj VARCHAR(20),
    rg VARCHAR(40),
    endereco TEXT,
    bloqueado BOOLEAN DEFAULT FALSE,
    criado_em TIMESTAMP
);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pet_parte_pet') THEN
        ALTER TABLE tb_peticionamento_parte
            ADD CONSTRAINT fk_pet_parte_pet
            FOREIGN KEY (peticionamento_id) REFERENCES tb_peticionamento_intermediario(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_pet_parte_pet ON tb_peticionamento_parte(peticionamento_id);

CREATE TABLE IF NOT EXISTS tb_peticionamento_anexo (
    id UUID PRIMARY KEY,
    peticionamento_id UUID NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    categoria VARCHAR(120),
    nome_original VARCHAR(260),
    content_type VARCHAR(120),
    tamanho_bytes BIGINT,
    documento_id UUID,
    arquivo BYTEA,
    criado_em TIMESTAMP
);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pet_anexo_pet') THEN
        ALTER TABLE tb_peticionamento_anexo
            ADD CONSTRAINT fk_pet_anexo_pet
            FOREIGN KEY (peticionamento_id) REFERENCES tb_peticionamento_intermediario(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pet_anexo_documento') THEN
        ALTER TABLE tb_peticionamento_anexo
            ADD CONSTRAINT fk_pet_anexo_documento
            FOREIGN KEY (documento_id) REFERENCES tb_documento_processual(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_pet_anexo_pet ON tb_peticionamento_anexo(peticionamento_id);
