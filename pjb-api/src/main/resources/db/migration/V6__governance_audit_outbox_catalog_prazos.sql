-- PJB Lote Governance: ledger (auditoria imutável), outbox, explainability, prazos.
-- Postgres target.

-- 1) Ledger append-only (cadeia de hash)
CREATE TABLE IF NOT EXISTS pjb_audit_ledger (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    actor_user_id BIGINT,
    actor_key VARCHAR(180),
    action VARCHAR(120) NOT NULL,
    resource_type VARCHAR(80),
    resource_id VARCHAR(120),
    justificativa TEXT,
    request_id VARCHAR(80),
    ip VARCHAR(80),
    user_agent VARCHAR(255),
    payload_hash VARCHAR(64),
    prev_hash VARCHAR(64),
    entry_hash VARCHAR(64) NOT NULL UNIQUE
);

CREATE INDEX IF NOT EXISTS idx_audit_ledger_resource ON pjb_audit_ledger(resource_type, resource_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_ledger_created ON pjb_audit_ledger(created_at);

-- 2) Outbox (idempotência de integração)
CREATE TABLE IF NOT EXISTS pjb_outbox_event (
    id BIGSERIAL PRIMARY KEY,
    dedup_key VARCHAR(160),
    event_type VARCHAR(80) NOT NULL,
    aggregate_type VARCHAR(80),
    aggregate_id VARCHAR(120),
    payload_json TEXT NOT NULL,
    headers_json TEXT,
    status VARCHAR(24) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP,
    locked_at TIMESTAMP,
    lock_owner VARCHAR(80),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_outbox_dedup ON pjb_outbox_event(dedup_key);
CREATE INDEX IF NOT EXISTS idx_outbox_status ON pjb_outbox_event(status, next_attempt_at, id);

-- 3) Explainability / rastreio de decisões automatizadas
CREATE TABLE IF NOT EXISTS pjb_decision_trace (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    actor_user_id BIGINT,
    actor_key VARCHAR(180),
    decision_type VARCHAR(80) NOT NULL,
    subject_type VARCHAR(80),
    subject_id VARCHAR(120),
    confidence NUMERIC(5,4),
    reasons_json TEXT,
    citations_json TEXT,
    input_digest VARCHAR(64),
    output_digest VARCHAR(64),
    request_id VARCHAR(80),
    model_version VARCHAR(80),
    metadata_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_decision_subject ON pjb_decision_trace(subject_type, subject_id, created_at);

-- 4) Calendário forense (feriados, suspensões)
CREATE TABLE IF NOT EXISTS pjb_calendario_forense (
    id BIGSERIAL PRIMARY KEY,
    uf VARCHAR(2),
    comarca VARCHAR(120),
    dia DATE NOT NULL,
    tipo VARCHAR(32) NOT NULL,
    descricao VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cal_forense_uf ON pjb_calendario_forense(uf, comarca, dia);

-- 5) Versionamento de catálogo (SSOT)
CREATE TABLE IF NOT EXISTS pjb_catalog_version (
    id BIGSERIAL PRIMARY KEY,
    catalog_key VARCHAR(80) NOT NULL,
    version VARCHAR(40) NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ux_cat_key_version UNIQUE(catalog_key, version)
);

CREATE INDEX IF NOT EXISTS idx_cat_key_active ON pjb_catalog_version(catalog_key, active);

-- 6) Processo: referência da versão aplicada
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS catalog_version_id BIGINT;

-- 7) Auditoria operacional (caso esteja ausente em ambientes já criados via ddl-auto)
CREATE TABLE IF NOT EXISTS tb_auditoria_evento (
    id BIGSERIAL PRIMARY KEY,
    usuario VARCHAR(255),
    acao VARCHAR(255) NOT NULL,
    alvo VARCHAR(255),
    data_hora TIMESTAMP NOT NULL DEFAULT NOW()
);
