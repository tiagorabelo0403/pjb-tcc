-- ==========================================================
-- V17 - Sigilo Access (solicitação/aprovação com senha), OAB strict, Perito nomeado, Órgãos oficiais
-- ==========================================================

-- 1) Usuário: campos OAB (advocacia) e normalização
ALTER TABLE tb_usuario
    ADD COLUMN IF NOT EXISTS oab VARCHAR(80);

ALTER TABLE tb_usuario
    ADD COLUMN IF NOT EXISTS oab_normalizada VARCHAR(80);

ALTER TABLE tb_usuario
    ADD COLUMN IF NOT EXISTS oab_uf VARCHAR(2);

ALTER TABLE tb_usuario
    ADD COLUMN IF NOT EXISTS oab_numero VARCHAR(20);

ALTER TABLE tb_usuario
    ADD COLUMN IF NOT EXISTS oab_sufixo VARCHAR(10);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_tb_usuario_oab_normalizada') THEN
        ALTER TABLE tb_usuario ADD CONSTRAINT uk_tb_usuario_oab_normalizada UNIQUE (oab_normalizada);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_tb_usuario_oab_uf ON tb_usuario(oab_uf);


-- 2) Solicitação de acesso a processo sigiloso (ADVOGADO sem vínculo) com senha (10 dias)
CREATE TABLE IF NOT EXISTS tb_sigilo_access_request (
    id UUID PRIMARY KEY,
    processo_id BIGINT NOT NULL,
    advogado_id BIGINT NOT NULL,

    motivo TEXT,
    status VARCHAR(20) NOT NULL,

    requested_at TIMESTAMP NOT NULL DEFAULT NOW(),
    approved_at TIMESTAMP,
    approved_by BIGINT,

    password_hash TEXT,
    expires_at TIMESTAMP,

    hide_approver BOOLEAN NOT NULL DEFAULT TRUE,
    rejected_reason TEXT,

    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP,
    last_used_at TIMESTAMP,

    revoked_at TIMESTAMP,
    revoked_by BIGINT
);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sigilo_req_processo') THEN
        ALTER TABLE tb_sigilo_access_request
            ADD CONSTRAINT fk_sigilo_req_processo
            FOREIGN KEY (processo_id) REFERENCES tb_processo(id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sigilo_req_advogado') THEN
        ALTER TABLE tb_sigilo_access_request
            ADD CONSTRAINT fk_sigilo_req_advogado
            FOREIGN KEY (advogado_id) REFERENCES tb_usuario(id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sigilo_req_approved_by') THEN
        ALTER TABLE tb_sigilo_access_request
            ADD CONSTRAINT fk_sigilo_req_approved_by
            FOREIGN KEY (approved_by) REFERENCES tb_usuario(id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sigilo_req_revoked_by') THEN
        ALTER TABLE tb_sigilo_access_request
            ADD CONSTRAINT fk_sigilo_req_revoked_by
            FOREIGN KEY (revoked_by) REFERENCES tb_usuario(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_sigilo_req_processo ON tb_sigilo_access_request(processo_id);
CREATE INDEX IF NOT EXISTS idx_sigilo_req_advogado ON tb_sigilo_access_request(advogado_id);
CREATE INDEX IF NOT EXISTS idx_sigilo_req_status ON tb_sigilo_access_request(status);
CREATE INDEX IF NOT EXISTS idx_sigilo_req_expires ON tb_sigilo_access_request(expires_at);


-- 3) Perito nomeado com acesso restrito
CREATE TABLE IF NOT EXISTS tb_perito_nomeacao (
    id BIGSERIAL PRIMARY KEY,
    processo_id BIGINT NOT NULL,
    perito_id BIGINT NOT NULL,

    nomeado_por BIGINT,
    status VARCHAR(20) NOT NULL,
    nomeado_em TIMESTAMP NOT NULL DEFAULT NOW(),

    revogado_em TIMESTAMP,
    revogado_por BIGINT,
    observacao TEXT,

    CONSTRAINT uk_perito_nomeacao_ativa UNIQUE (processo_id, perito_id, status)
);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_perito_nomeacao_processo') THEN
        ALTER TABLE tb_perito_nomeacao
            ADD CONSTRAINT fk_perito_nomeacao_processo
            FOREIGN KEY (processo_id) REFERENCES tb_processo(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_perito_nomeacao_perito') THEN
        ALTER TABLE tb_perito_nomeacao
            ADD CONSTRAINT fk_perito_nomeacao_perito
            FOREIGN KEY (perito_id) REFERENCES tb_usuario(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_perito_nomeacao_nomeado_por') THEN
        ALTER TABLE tb_perito_nomeacao
            ADD CONSTRAINT fk_perito_nomeacao_nomeado_por
            FOREIGN KEY (nomeado_por) REFERENCES tb_usuario(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_perito_nomeacao_revogado_por') THEN
        ALTER TABLE tb_perito_nomeacao
            ADD CONSTRAINT fk_perito_nomeacao_revogado_por
            FOREIGN KEY (revogado_por) REFERENCES tb_usuario(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_perito_nomeacao_processo ON tb_perito_nomeacao(processo_id);
CREATE INDEX IF NOT EXISTS idx_perito_nomeacao_perito ON tb_perito_nomeacao(perito_id);


-- 4) Órgãos oficiais sempre (tabela de bindings por processo)
CREATE TABLE IF NOT EXISTS tb_processo_orgao_oficial (
    id BIGSERIAL PRIMARY KEY,
    processo_id BIGINT NOT NULL,
    orgao_tipo VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_processo_orgao_oficial UNIQUE (processo_id, orgao_tipo)
);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_processo_orgao_oficial_processo') THEN
        ALTER TABLE tb_processo_orgao_oficial
            ADD CONSTRAINT fk_processo_orgao_oficial_processo
            FOREIGN KEY (processo_id) REFERENCES tb_processo(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_processo_orgao_oficial_processo ON tb_processo_orgao_oficial(processo_id);
CREATE INDEX IF NOT EXISTS idx_processo_orgao_oficial_tipo ON tb_processo_orgao_oficial(orgao_tipo);
