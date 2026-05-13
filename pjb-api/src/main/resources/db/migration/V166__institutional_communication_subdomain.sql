CREATE TABLE IF NOT EXISTS tb_inst_catalog_unit_snapshot (
    id BIGSERIAL PRIMARY KEY,
    ver BIGINT NOT NULL DEFAULT 0,
    codigo_unidade VARCHAR(180) NOT NULL,
    destinatario_kind VARCHAR(80) NOT NULL,
    uf VARCHAR(8),
    comarca VARCHAR(160),
    foro VARCHAR(160),
    ramo_direito VARCHAR(80),
    grau_jurisdicao VARCHAR(80),
    ativa BOOLEAN NOT NULL,
    vigencia_inicio TIMESTAMP NOT NULL,
    vigencia_fim TIMESTAMP,
    snapshot_hash VARCHAR(128) NOT NULL,
    snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_inst_catalog_unit_codigo_vigencia UNIQUE (codigo_unidade, vigencia_inicio)
);

CREATE INDEX IF NOT EXISTS idx_inst_catalog_unit_kind_uf
    ON tb_inst_catalog_unit_snapshot (destinatario_kind, uf);
CREATE INDEX IF NOT EXISTS idx_inst_catalog_unit_comarca
    ON tb_inst_catalog_unit_snapshot (uf, comarca, foro);

CREATE TABLE IF NOT EXISTS tb_inst_inbox_item_snapshot (
    id BIGSERIAL PRIMARY KEY,
    ver BIGINT NOT NULL DEFAULT 0,
    inbox_item_id VARCHAR(160) NOT NULL,
    expedicao_uuid VARCHAR(160) NOT NULL,
    processo_id BIGINT NOT NULL,
    unidade_codigo VARCHAR(180) NOT NULL,
    caixa_codigo_atual VARCHAR(180) NOT NULL,
    status_codigo VARCHAR(80) NOT NULL,
    prazo_ciencia_em TIMESTAMP,
    prazo_resposta_em TIMESTAMP,
    snapshot_hash VARCHAR(128) NOT NULL,
    snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_inst_inbox_item_expedicao UNIQUE (expedicao_uuid)
);

CREATE INDEX IF NOT EXISTS idx_inst_inbox_item_proc
    ON tb_inst_inbox_item_snapshot (processo_id, status_codigo);
CREATE INDEX IF NOT EXISTS idx_inst_inbox_item_unidade_caixa
    ON tb_inst_inbox_item_snapshot (unidade_codigo, caixa_codigo_atual, status_codigo);

CREATE TABLE IF NOT EXISTS tb_inst_delivery_job_snapshot (
    id BIGSERIAL PRIMARY KEY,
    ver BIGINT NOT NULL DEFAULT 0,
    job_id VARCHAR(160) NOT NULL,
    expedicao_uuid VARCHAR(160) NOT NULL,
    processo_id BIGINT,
    unidade_codigo VARCHAR(180) NOT NULL,
    caixa_codigo VARCHAR(180) NOT NULL,
    status_codigo VARCHAR(80) NOT NULL,
    canal_corrente VARCHAR(80) NOT NULL,
    attempt_count INTEGER NOT NULL,
    next_attempt_at TIMESTAMP NOT NULL,
    terminal_at TIMESTAMP,
    snapshot_hash VARCHAR(128) NOT NULL,
    snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_inst_delivery_job_jobid UNIQUE (job_id)
);

CREATE INDEX IF NOT EXISTS idx_inst_delivery_job_status_next
    ON tb_inst_delivery_job_snapshot (status_codigo, next_attempt_at);
CREATE INDEX IF NOT EXISTS idx_inst_delivery_job_proc
    ON tb_inst_delivery_job_snapshot (processo_id, status_codigo);

CREATE TABLE IF NOT EXISTS tb_inst_delivery_attempt_snapshot (
    id BIGSERIAL PRIMARY KEY,
    ver BIGINT NOT NULL DEFAULT 0,
    attempt_id VARCHAR(160) NOT NULL,
    job_id VARCHAR(160) NOT NULL,
    expedicao_uuid VARCHAR(160) NOT NULL,
    attempt_number INTEGER NOT NULL,
    status_codigo VARCHAR(80) NOT NULL,
    channel_code VARCHAR(80) NOT NULL,
    ended_at TIMESTAMP NOT NULL,
    snapshot_hash VARCHAR(128) NOT NULL,
    snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_inst_delivery_attempt_id UNIQUE (attempt_id)
);

CREATE INDEX IF NOT EXISTS idx_inst_delivery_attempt_job
    ON tb_inst_delivery_attempt_snapshot (job_id, attempt_number);
CREATE INDEX IF NOT EXISTS idx_inst_delivery_attempt_expedicao
    ON tb_inst_delivery_attempt_snapshot (expedicao_uuid, ended_at);

CREATE TABLE IF NOT EXISTS tb_inst_dead_letter_snapshot (
    id BIGSERIAL PRIMARY KEY,
    ver BIGINT NOT NULL DEFAULT 0,
    entry_id VARCHAR(160) NOT NULL,
    job_id VARCHAR(160) NOT NULL,
    expedicao_uuid VARCHAR(160) NOT NULL,
    processo_id BIGINT,
    reason_code VARCHAR(80),
    channel_code VARCHAR(80) NOT NULL,
    snapshot_hash VARCHAR(128) NOT NULL,
    snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_inst_dead_letter_entry UNIQUE (entry_id)
);

CREATE INDEX IF NOT EXISTS idx_inst_dead_letter_proc
    ON tb_inst_dead_letter_snapshot (processo_id, created_at);
CREATE INDEX IF NOT EXISTS idx_inst_dead_letter_expedicao
    ON tb_inst_dead_letter_snapshot (expedicao_uuid, created_at);

CREATE TABLE IF NOT EXISTS tb_inst_timeline_event_snapshot (
    id BIGSERIAL PRIMARY KEY,
    ver BIGINT NOT NULL DEFAULT 0,
    event_id VARCHAR(160) NOT NULL,
    expedicao_uuid VARCHAR(160) NOT NULL,
    processo_id BIGINT NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    status_codigo VARCHAR(80) NOT NULL,
    unidade_codigo VARCHAR(180) NOT NULL,
    caixa_codigo VARCHAR(180) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    snapshot_hash VARCHAR(128) NOT NULL,
    snapshot_json TEXT NOT NULL,
    CONSTRAINT uk_inst_timeline_event_eventid UNIQUE (event_id)
);

CREATE INDEX IF NOT EXISTS idx_inst_timeline_event_expedicao
    ON tb_inst_timeline_event_snapshot (expedicao_uuid, occurred_at);
CREATE INDEX IF NOT EXISTS idx_inst_timeline_event_proc
    ON tb_inst_timeline_event_snapshot (processo_id, occurred_at);

CREATE TABLE IF NOT EXISTS tb_inst_delivery_proof_snapshot (
    id BIGSERIAL PRIMARY KEY,
    ver BIGINT NOT NULL DEFAULT 0,
    proof_id VARCHAR(160) NOT NULL,
    expedicao_uuid VARCHAR(160) NOT NULL,
    processo_id BIGINT NOT NULL,
    etapa VARCHAR(120) NOT NULL,
    canal VARCHAR(80) NOT NULL,
    evidencia_tipo VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    snapshot_hash VARCHAR(128) NOT NULL,
    snapshot_json TEXT NOT NULL,
    CONSTRAINT uk_inst_delivery_proof_proofid UNIQUE (proof_id)
);

CREATE INDEX IF NOT EXISTS idx_inst_delivery_proof_expedicao
    ON tb_inst_delivery_proof_snapshot (expedicao_uuid, created_at);
CREATE INDEX IF NOT EXISTS idx_inst_delivery_proof_proc
    ON tb_inst_delivery_proof_snapshot (processo_id, etapa);

CREATE TABLE IF NOT EXISTS tb_inst_gate_state_snapshot (
    id BIGSERIAL PRIMARY KEY,
    ver BIGINT NOT NULL DEFAULT 0,
    gate_state_id VARCHAR(160) NOT NULL,
    expedicao_uuid VARCHAR(160) NOT NULL,
    processo_id BIGINT NOT NULL,
    gate_code VARCHAR(160) NOT NULL,
    status_codigo VARCHAR(80) NOT NULL,
    bloqueado BOOLEAN NOT NULL,
    released_at TIMESTAMP,
    snapshot_hash VARCHAR(128) NOT NULL,
    snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_inst_gate_state_expedicao UNIQUE (expedicao_uuid)
);

CREATE INDEX IF NOT EXISTS idx_inst_gate_state_proc
    ON tb_inst_gate_state_snapshot (processo_id, status_codigo);
CREATE INDEX IF NOT EXISTS idx_inst_gate_state_gate
    ON tb_inst_gate_state_snapshot (gate_code, status_codigo);

CREATE TABLE IF NOT EXISTS tb_inst_external_dispatch_snapshot (
    id BIGSERIAL PRIMARY KEY,
    ver BIGINT NOT NULL DEFAULT 0,
    dispatch_id VARCHAR(160) NOT NULL,
    job_id VARCHAR(160) NOT NULL,
    expedicao_uuid VARCHAR(160) NOT NULL,
    processo_id BIGINT,
    canal_codigo VARCHAR(80) NOT NULL,
    provider_codigo VARCHAR(80) NOT NULL,
    status_codigo VARCHAR(80) NOT NULL,
    provider_reference VARCHAR(255),
    failure_reason TEXT,
    payload_hash VARCHAR(128) NOT NULL,
    snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_inst_external_dispatch_id UNIQUE (dispatch_id)
);

CREATE INDEX IF NOT EXISTS idx_inst_external_dispatch_exp
    ON tb_inst_external_dispatch_snapshot (expedicao_uuid, updated_at);
CREATE INDEX IF NOT EXISTS idx_inst_external_dispatch_proc
    ON tb_inst_external_dispatch_snapshot (processo_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_inst_external_dispatch_status
    ON tb_inst_external_dispatch_snapshot (status_codigo, updated_at);

CREATE TABLE IF NOT EXISTS tb_inst_catalog_governance_snapshot (
    id BIGSERIAL PRIMARY KEY,
    ver BIGINT NOT NULL DEFAULT 0,
    governance_id VARCHAR(180) NOT NULL,
    unidade_codigo VARCHAR(180) NOT NULL,
    destinatario_kind VARCHAR(80) NOT NULL,
    uf VARCHAR(8),
    comarca VARCHAR(160),
    foro VARCHAR(160),
    ramo_direito VARCHAR(80),
    grau_jurisdicao VARCHAR(80),
    abrangencia VARCHAR(40) NOT NULL,
    ativa BOOLEAN NOT NULL,
    suspende_entrega_externa BOOLEAN NOT NULL,
    exige_homologacao_admin BOOLEAN NOT NULL,
    unidade_substituta_codigo VARCHAR(180),
    vigencia_inicio TIMESTAMP NOT NULL,
    vigencia_fim TIMESTAMP,
    snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_inst_catalog_governance_id UNIQUE (governance_id)
);

CREATE INDEX IF NOT EXISTS idx_inst_catalog_governance_unit
    ON tb_inst_catalog_governance_snapshot (unidade_codigo, vigencia_inicio);
CREATE INDEX IF NOT EXISTS idx_inst_catalog_governance_kind_uf
    ON tb_inst_catalog_governance_snapshot (destinatario_kind, uf);

CREATE TABLE IF NOT EXISTS tb_inst_competence_rule_snapshot (
    id BIGSERIAL PRIMARY KEY,
    ver BIGINT NOT NULL DEFAULT 0,
    rule_id VARCHAR(180) NOT NULL,
    destinatario_kind VARCHAR(80) NOT NULL,
    papel_processual VARCHAR(80) NOT NULL,
    uf VARCHAR(8),
    comarca VARCHAR(160),
    foro VARCHAR(160),
    ramo_direito VARCHAR(80),
    grau_jurisdicao VARCHAR(80),
    unidade_codigo VARCHAR(180) NOT NULL,
    prioridade INTEGER NOT NULL,
    ativa BOOLEAN NOT NULL,
    vigencia_inicio TIMESTAMP NOT NULL,
    vigencia_fim TIMESTAMP,
    snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_inst_comp_rule_id UNIQUE (rule_id)
);

CREATE INDEX IF NOT EXISTS idx_inst_comp_rule_kind_papel
    ON tb_inst_competence_rule_snapshot (destinatario_kind, papel_processual, prioridade);
CREATE INDEX IF NOT EXISTS idx_inst_comp_rule_local
    ON tb_inst_competence_rule_snapshot (uf, comarca, foro);
