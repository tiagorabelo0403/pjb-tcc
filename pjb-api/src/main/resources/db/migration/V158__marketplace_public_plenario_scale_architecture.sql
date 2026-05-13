CREATE TABLE IF NOT EXISTS tb_marketplace_integration_plan (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    display_name VARCHAR(160) NOT NULL,
    status VARCHAR(40) NOT NULL,
    max_protocolos_dia INTEGER NOT NULL DEFAULT 500,
    max_webhook_endpoints INTEGER NOT NULL DEFAULT 3,
    allow_streaming BOOLEAN NOT NULL DEFAULT FALSE,
    allow_high_volume BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS tb_marketplace_client_subscription (
    id BIGSERIAL PRIMARY KEY,
    client_app_id BIGINT NOT NULL REFERENCES tb_marketplace_client_app(id),
    plan_id BIGINT NOT NULL REFERENCES tb_marketplace_integration_plan(id),
    status VARCHAR(40) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ends_at TIMESTAMPTZ,
    protocolos_dia_atual INTEGER NOT NULL DEFAULT 0,
    ultimo_reset_contador TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    webhook_endpoint_limit_override INTEGER,
    observacoes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_marketplace_subscription_client ON tb_marketplace_client_subscription (client_app_id);
CREATE INDEX IF NOT EXISTS idx_marketplace_subscription_status ON tb_marketplace_client_subscription (status);

CREATE TABLE IF NOT EXISTS tb_marketplace_webhook_endpoint (
    id BIGSERIAL PRIMARY KEY,
    client_app_id BIGINT NOT NULL REFERENCES tb_marketplace_client_app(id),
    subscription_id BIGINT REFERENCES tb_marketplace_client_subscription(id),
    callback_url VARCHAR(320) NOT NULL,
    event_filter VARCHAR(260) NOT NULL,
    signing_secret_hash VARCHAR(128) NOT NULL,
    status VARCHAR(40) NOT NULL,
    last_success_at TIMESTAMPTZ,
    last_failure_at TIMESTAMPTZ,
    last_error_message VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_marketplace_webhook_client ON tb_marketplace_webhook_endpoint (client_app_id);
CREATE INDEX IF NOT EXISTS idx_marketplace_webhook_status ON tb_marketplace_webhook_endpoint (status);

CREATE TABLE IF NOT EXISTS tb_marketplace_webhook_delivery (
    id BIGSERIAL PRIMARY KEY,
    endpoint_id BIGINT NOT NULL REFERENCES tb_marketplace_webhook_endpoint(id),
    event_type VARCHAR(80) NOT NULL,
    payload_hash VARCHAR(128) NOT NULL,
    status VARCHAR(40) NOT NULL,
    response_code INTEGER,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    response_excerpt VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_marketplace_webhook_delivery_endpoint ON tb_marketplace_webhook_delivery (endpoint_id);
CREATE INDEX IF NOT EXISTS idx_marketplace_webhook_delivery_status ON tb_marketplace_webhook_delivery (status);

CREATE TABLE IF NOT EXISTS tb_public_plenario_media_asset (
    id BIGSERIAL PRIMARY KEY,
    sessao_id BIGINT NOT NULL REFERENCES tb_julgamento_colegiado(id),
    uploaded_by_id BIGINT REFERENCES tb_usuario(id),
    tipo VARCHAR(80) NOT NULL,
    titulo VARCHAR(180) NOT NULL,
    url_publica VARCHAR(320) NOT NULL,
    hash_integridade VARCHAR(128),
    publico BOOLEAN NOT NULL DEFAULT TRUE,
    ordem_exibicao INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_public_plenario_media_sessao ON tb_public_plenario_media_asset (sessao_id);
CREATE INDEX IF NOT EXISTS idx_public_plenario_media_publico ON tb_public_plenario_media_asset (publico);

CREATE TABLE IF NOT EXISTS tb_public_plenario_esclarecimento_fato (
    id BIGSERIAL PRIMARY KEY,
    sessao_id BIGINT NOT NULL REFERENCES tb_julgamento_colegiado(id),
    solicitante_id BIGINT REFERENCES tb_usuario(id),
    respondido_por_id BIGINT REFERENCES tb_usuario(id),
    resumo_duvida TEXT NOT NULL,
    resposta_publica TEXT,
    status VARCHAR(40) NOT NULL,
    visivel_publicamente BOOLEAN NOT NULL DEFAULT FALSE,
    respondido_em TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_public_plenario_esclarecimento_sessao ON tb_public_plenario_esclarecimento_fato (sessao_id);
CREATE INDEX IF NOT EXISTS idx_public_plenario_esclarecimento_status ON tb_public_plenario_esclarecimento_fato (status);

CREATE TABLE IF NOT EXISTS tb_cache_policy_override (
    id BIGSERIAL PRIMARY KEY,
    cache_name VARCHAR(120) NOT NULL,
    role_name VARCHAR(120) NOT NULL,
    ttl_seconds INTEGER NOT NULL,
    stale_while_revalidate_seconds INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT uk_cache_policy_override UNIQUE (cache_name, role_name)
);

CREATE INDEX IF NOT EXISTS idx_cache_policy_override_enabled ON tb_cache_policy_override (enabled);

CREATE TABLE IF NOT EXISTS tb_partition_plan (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(120) NOT NULL UNIQUE,
    partition_column VARCHAR(120) NOT NULL,
    partition_prefix VARCHAR(120) NOT NULL,
    start_year INTEGER NOT NULL,
    years_ahead INTEGER NOT NULL DEFAULT 2,
    status VARCHAR(40) NOT NULL,
    last_materialized_year INTEGER,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_partition_plan_status ON tb_partition_plan (status);
