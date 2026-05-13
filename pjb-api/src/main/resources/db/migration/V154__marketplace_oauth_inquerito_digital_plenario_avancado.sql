CREATE TABLE IF NOT EXISTS tb_marketplace_client_app (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(120) NOT NULL UNIQUE,
    client_secret_hash VARCHAR(128) NOT NULL,
    display_name VARCHAR(180) NOT NULL,
    owner_name VARCHAR(180),
    owner_email VARCHAR(180),
    allowed_scopes VARCHAR(500) NOT NULL,
    allowed_grants VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL,
    trusted_origin VARCHAR(220),
    access_token_ttl_seconds INTEGER,
    last_authenticated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_marketplace_client_app_status ON tb_marketplace_client_app (status);

CREATE TABLE IF NOT EXISTS tb_marketplace_access_token (
    id BIGSERIAL PRIMARY KEY,
    jti VARCHAR(120) NOT NULL UNIQUE,
    client_app_id BIGINT NOT NULL,
    scope VARCHAR(500) NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    status VARCHAR(40) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    last_introspection_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_marketplace_access_token_client FOREIGN KEY (client_app_id) REFERENCES tb_marketplace_client_app (id)
);

CREATE INDEX IF NOT EXISTS idx_marketplace_access_token_status ON tb_marketplace_access_token (status);
CREATE INDEX IF NOT EXISTS idx_marketplace_access_token_client ON tb_marketplace_access_token (client_app_id, status);

CREATE TABLE IF NOT EXISTS tb_marketplace_audit_event (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    client_app_id BIGINT,
    jti VARCHAR(120),
    subject VARCHAR(180),
    event_summary TEXT,
    ip_address VARCHAR(64),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_marketplace_audit_event_client FOREIGN KEY (client_app_id) REFERENCES tb_marketplace_client_app (id)
);

CREATE INDEX IF NOT EXISTS idx_marketplace_audit_event_type ON tb_marketplace_audit_event (event_type);
CREATE INDEX IF NOT EXISTS idx_marketplace_audit_event_client ON tb_marketplace_audit_event (client_app_id, created_at);

CREATE TABLE IF NOT EXISTS tb_inquerito_policial_digital (
    id BIGSERIAL PRIMARY KEY,
    numero_procedimento VARCHAR(80) NOT NULL UNIQUE,
    tipo VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    fase_atual VARCHAR(40) NOT NULL,
    natureza_fato VARCHAR(180) NOT NULL,
    resumo_fatos TEXT NOT NULL,
    investigados_resumo TEXT,
    vitimas_resumo TEXT,
    indicios_resumo TEXT,
    diligencias_pendentes TEXT,
    ultima_movimentacao_resumo TEXT,
    cadeia_custodia_hash VARCHAR(128),
    orgao_apuracao VARCHAR(120),
    uf VARCHAR(2),
    municipio VARCHAR(120),
    nivel_sigilo VARCHAR(40),
    autoridade_responsavel_id BIGINT,
    processo_vinculado_id BIGINT,
    instaurado_em TIMESTAMPTZ,
    remetido_ao_mp_em TIMESTAMPTZ,
    prazo_conclusao DATE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inquerito_autoridade FOREIGN KEY (autoridade_responsavel_id) REFERENCES tb_usuario (id),
    CONSTRAINT fk_inquerito_processo FOREIGN KEY (processo_vinculado_id) REFERENCES tb_processo (id)
);

CREATE INDEX IF NOT EXISTS idx_inquerito_status ON tb_inquerito_policial_digital (status);
CREATE INDEX IF NOT EXISTS idx_inquerito_autoridade ON tb_inquerito_policial_digital (autoridade_responsavel_id, status);
CREATE INDEX IF NOT EXISTS idx_inquerito_processo ON tb_inquerito_policial_digital (processo_vinculado_id);

CREATE TABLE IF NOT EXISTS tb_plenario_virtual_sessao (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(120) NOT NULL UNIQUE,
    processo_id BIGINT NOT NULL,
    relator_id BIGINT NOT NULL,
    orgao_julgador VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL,
    materia_resumo TEXT,
    observacoes TEXT,
    segredo_ate_proclamacao BOOLEAN NOT NULL DEFAULT TRUE,
    quorum_minimo INTEGER,
    votos_recebidos INTEGER,
    votos_acompanham_relator INTEGER,
    votos_divergentes INTEGER,
    votos_parciais INTEGER,
    resultado_final VARCHAR(180),
    ata_hash VARCHAR(128),
    prova_integridade_raiz VARCHAR(128),
    aberta_em TIMESTAMPTZ,
    proclamada_em TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_plenario_virtual_sessao_processo FOREIGN KEY (processo_id) REFERENCES tb_processo (id),
    CONSTRAINT fk_plenario_virtual_sessao_relator FOREIGN KEY (relator_id) REFERENCES tb_usuario (id)
);

CREATE INDEX IF NOT EXISTS idx_plenario_virtual_sessao_status ON tb_plenario_virtual_sessao (status);
CREATE INDEX IF NOT EXISTS idx_plenario_virtual_sessao_processo ON tb_plenario_virtual_sessao (processo_id);

CREATE TABLE IF NOT EXISTS tb_plenario_virtual_voto (
    id BIGSERIAL PRIMARY KEY,
    sessao_id BIGINT NOT NULL,
    ministro_id BIGINT NOT NULL,
    commitment_hash VARCHAR(128) NOT NULL,
    receipt_hash VARCHAR(128) NOT NULL,
    envelope_base64 TEXT NOT NULL,
    prova_integridade TEXT NOT NULL,
    status VARCHAR(40) NOT NULL,
    fundamentacao_resumo TEXT,
    ressalva TEXT,
    sigilo_ate_proclamacao BOOLEAN NOT NULL DEFAULT TRUE,
    revelado_em TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_plenario_virtual_voto_sessao FOREIGN KEY (sessao_id) REFERENCES tb_plenario_virtual_sessao (id),
    CONSTRAINT fk_plenario_virtual_voto_ministro FOREIGN KEY (ministro_id) REFERENCES tb_usuario (id),
    CONSTRAINT uk_plenario_virtual_voto_sessao_ministro UNIQUE (sessao_id, ministro_id)
);

CREATE INDEX IF NOT EXISTS idx_plenario_virtual_voto_sessao ON tb_plenario_virtual_voto (sessao_id);
CREATE INDEX IF NOT EXISTS idx_plenario_virtual_voto_ministro ON tb_plenario_virtual_voto (ministro_id);
