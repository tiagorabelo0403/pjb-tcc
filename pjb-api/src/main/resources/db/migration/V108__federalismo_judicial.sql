CREATE TABLE IF NOT EXISTS tb_no_federacao_judicial (
    id BIGSERIAL PRIMARY KEY,
    codigo_tribunal VARCHAR(20) NOT NULL,
    nome VARCHAR(180) NOT NULL,
    uf VARCHAR(2),
    tipo_justica VARCHAR(30) NOT NULL,
    endpoint_principal VARCHAR(240) NOT NULL,
    endpoint_backup VARCHAR(240),
    kafka_brokers VARCHAR(500),
    chave_publica_base64 TEXT,
    chave_publica_fingerprint VARCHAR(64),
    status_atual VARCHAR(30) NOT NULL,
    ultima_heartbeat_em TIMESTAMP,
    ultima_sincronizacao_em TIMESTAMP,
    ultima_falha_em TIMESTAMP,
    backlog_pendente BIGINT NOT NULL DEFAULT 0,
    capacidade_backlog BIGINT NOT NULL DEFAULT 10000,
    operacao_autonoma_ativa BOOLEAN NOT NULL DEFAULT TRUE,
    aceita_recepcao_eventos BOOLEAN NOT NULL DEFAULT TRUE,
    versao_schema_atual BIGINT NOT NULL DEFAULT 1,
    regiao VARCHAR(60),
    zona VARCHAR(60),
    prioridade_failover INTEGER NOT NULL DEFAULT 0,
    clock_logico BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    versao BIGINT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_no_federacao_judicial_codigo ON tb_no_federacao_judicial (codigo_tribunal);
CREATE INDEX IF NOT EXISTS idx_no_federacao_status ON tb_no_federacao_judicial (status_atual, ultima_heartbeat_em);
CREATE INDEX IF NOT EXISTS idx_no_federacao_tipo ON tb_no_federacao_judicial (tipo_justica, uf);
CREATE INDEX IF NOT EXISTS idx_no_federacao_regiao ON tb_no_federacao_judicial (regiao, zona);
CREATE INDEX IF NOT EXISTS idx_no_federacao_schema ON tb_no_federacao_judicial (versao_schema_atual);

CREATE TABLE IF NOT EXISTS tb_no_federacao_topico (
    no_id BIGINT NOT NULL,
    topico VARCHAR(180) NOT NULL,
    CONSTRAINT fk_no_federacao_topico_no FOREIGN KEY (no_id) REFERENCES tb_no_federacao_judicial(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_no_federacao_topico_no ON tb_no_federacao_topico (no_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_no_federacao_topico_item ON tb_no_federacao_topico (no_id, topico);

CREATE TABLE IF NOT EXISTS tb_no_federacao_capacidade (
    no_id BIGINT NOT NULL,
    capacidade VARCHAR(120) NOT NULL,
    CONSTRAINT fk_no_federacao_capacidade_no FOREIGN KEY (no_id) REFERENCES tb_no_federacao_judicial(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_no_federacao_capacidade_no ON tb_no_federacao_capacidade (no_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_no_federacao_capacidade_item ON tb_no_federacao_capacidade (no_id, capacidade);

CREATE TABLE IF NOT EXISTS tb_federacao_ledger_entry (
    id BIGSERIAL PRIMARY KEY,
    sequencia_global BIGINT NOT NULL,
    sequencia_tribunal BIGINT NOT NULL,
    hash_entrada VARCHAR(64) NOT NULL,
    hash_anterior VARCHAR(64) NOT NULL,
    tribunal_codigo VARCHAR(20) NOT NULL,
    tipo_evento VARCHAR(120) NOT NULL,
    topic_kafka VARCHAR(180) NOT NULL,
    nupn VARCHAR(50),
    payload_hash VARCHAR(64) NOT NULL,
    operador_id VARCHAR(120),
    schema_version BIGINT NOT NULL DEFAULT 1,
    correlation_id VARCHAR(120),
    idempotency_key VARCHAR(180) NOT NULL,
    status_assinatura VARCHAR(20) NOT NULL,
    classificacao_conflito VARCHAR(20) NOT NULL,
    tamanho_payload_bytes INTEGER NOT NULL DEFAULT 0,
    metadata_json TEXT,
    ocorrido_em TIMESTAMP NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_federacao_ledger_hash ON tb_federacao_ledger_entry (hash_entrada);
CREATE UNIQUE INDEX IF NOT EXISTS uk_federacao_ledger_idempotency ON tb_federacao_ledger_entry (idempotency_key);
CREATE INDEX IF NOT EXISTS idx_federacao_ledger_seq_global ON tb_federacao_ledger_entry (sequencia_global);
CREATE INDEX IF NOT EXISTS idx_federacao_ledger_nupn ON tb_federacao_ledger_entry (nupn);
CREATE INDEX IF NOT EXISTS idx_federacao_ledger_tribunal_seq ON tb_federacao_ledger_entry (tribunal_codigo, sequencia_tribunal);
CREATE INDEX IF NOT EXISTS idx_federacao_ledger_evento ON tb_federacao_ledger_entry (tipo_evento, topic_kafka, ocorrido_em);

CREATE TABLE IF NOT EXISTS tb_federacao_evento_outbox (
    id UUID PRIMARY KEY,
    tribunal_codigo VARCHAR(20) NOT NULL,
    topic_kafka VARCHAR(180) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    payload_json TEXT NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(180) NOT NULL,
    correlation_id VARCHAR(120),
    schema_version BIGINT NOT NULL DEFAULT 1,
    tentativas INTEGER NOT NULL DEFAULT 0,
    prioridade INTEGER NOT NULL DEFAULT 0,
    proxima_tentativa_em TIMESTAMP NOT NULL,
    publicado_em TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    ultimo_erro TEXT,
    criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_federacao_outbox_idempotency ON tb_federacao_evento_outbox (idempotency_key);
CREATE INDEX IF NOT EXISTS idx_federacao_outbox_status ON tb_federacao_evento_outbox (status, proxima_tentativa_em);
CREATE INDEX IF NOT EXISTS idx_federacao_outbox_tribunal ON tb_federacao_evento_outbox (tribunal_codigo, topic_kafka);
CREATE INDEX IF NOT EXISTS idx_federacao_outbox_corr ON tb_federacao_evento_outbox (correlation_id);
