ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS numero VARCHAR(255);
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS rito VARCHAR(255);
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS peticao_inicial_text TEXT;
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS analise_triagem_v1 TEXT;
ALTER TABLE tb_processo ALTER COLUMN objeto_processual TYPE TEXT;
ALTER TABLE tb_processo ALTER COLUMN pedido_principal TYPE TEXT;
ALTER TABLE tb_processo ALTER COLUMN pedidos_consolidados TYPE TEXT;
ALTER TABLE tb_processo ALTER COLUMN material_probatorio_resumo TYPE TEXT;
ALTER TABLE tb_processo ALTER COLUMN material_probatorio_hash TYPE VARCHAR(128);
ALTER TABLE tb_processo ALTER COLUMN janela_acordo_resumo TYPE VARCHAR(4000);
ALTER TABLE tb_processo ALTER COLUMN routing_confidence TYPE NUMERIC(10, 4);
ALTER TABLE tb_processo ALTER COLUMN connector_submission_attempts DROP NOT NULL;
ALTER TABLE tb_processo ALTER COLUMN connector_sync_attempts DROP NOT NULL;

ALTER TABLE tb_usuario ADD COLUMN IF NOT EXISTS especialidades_raw VARCHAR(2000);
ALTER TABLE tb_usuario ALTER COLUMN senha DROP NOT NULL;

ALTER TABLE jurisdicoes ADD COLUMN IF NOT EXISTS municipio_sede VARCHAR(100);
ALTER TABLE jurisdicoes ADD COLUMN IF NOT EXISTS foro VARCHAR(120);
ALTER TABLE jurisdicoes ADD COLUMN IF NOT EXISTS secao_judiciaria VARCHAR(120);
ALTER TABLE jurisdicoes ADD COLUMN IF NOT EXISTS subsecao_judiciaria VARCHAR(120);
ALTER TABLE jurisdicoes ADD COLUMN IF NOT EXISTS circunscricao VARCHAR(120);
ALTER TABLE jurisdicoes ALTER COLUMN codigo TYPE VARCHAR(50);
ALTER TABLE jurisdicoes ALTER COLUMN sigla TYPE VARCHAR(20);
ALTER TABLE jurisdicoes ALTER COLUMN nome TYPE VARCHAR(200);
ALTER TABLE jurisdicoes ALTER COLUMN competencia_material TYPE VARCHAR(1000);
ALTER TABLE jurisdicoes ALTER COLUMN competencia_territorial TYPE VARCHAR(500);

ALTER TABLE tb_inst_delivery_job_snapshot ADD COLUMN IF NOT EXISTS destinatario_kind_codigo VARCHAR(80);
UPDATE tb_inst_delivery_job_snapshot
   SET destinatario_kind_codigo = 'UNKNOWN'
 WHERE destinatario_kind_codigo IS NULL;
ALTER TABLE tb_inst_delivery_job_snapshot ALTER COLUMN destinatario_kind_codigo SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_inst_delivery_job_unidade_kind
    ON tb_inst_delivery_job_snapshot (unidade_codigo, destinatario_kind_codigo, status_codigo);

ALTER TABLE tb_inst_affiliation_snapshot ADD COLUMN IF NOT EXISTS organization_scope VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_inst_affiliation_scope
    ON tb_inst_affiliation_snapshot (organization_scope, updated_at);

CREATE TABLE IF NOT EXISTS tb_inst_official_source_attestation_snapshot (
    id BIGSERIAL PRIMARY KEY,
    ver BIGINT NOT NULL DEFAULT 0,
    subject_type VARCHAR(40) NOT NULL,
    subject_id VARCHAR(180) NOT NULL,
    affiliation_id VARCHAR(180),
    request_id VARCHAR(180),
    organization_scope VARCHAR(80),
    orgao_sigla VARCHAR(80),
    unidade_codigo VARCHAR(180),
    public_recognition_status VARCHAR(80) NOT NULL,
    attestation_status VARCHAR(80) NOT NULL,
    sovereign_recognition_ready BOOLEAN NOT NULL,
    due_now BOOLEAN NOT NULL,
    automatic_refresh_eligible BOOLEAN NOT NULL,
    last_attested_at TIMESTAMP NOT NULL,
    next_refresh_at TIMESTAMP,
    integrity_hash VARCHAR(128) NOT NULL,
    snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_inst_off_source_att_subject UNIQUE (subject_type, subject_id)
);

CREATE INDEX IF NOT EXISTS idx_inst_off_source_att_subject
    ON tb_inst_official_source_attestation_snapshot (subject_type, subject_id);
CREATE INDEX IF NOT EXISTS idx_inst_off_source_att_aff
    ON tb_inst_official_source_attestation_snapshot (affiliation_id);
CREATE INDEX IF NOT EXISTS idx_inst_off_source_att_req
    ON tb_inst_official_source_attestation_snapshot (request_id);
CREATE INDEX IF NOT EXISTS idx_inst_off_source_att_due
    ON tb_inst_official_source_attestation_snapshot (next_refresh_at, due_now);

CREATE TABLE IF NOT EXISTS tb_inst_affiliation_request_snapshot (
    id BIGSERIAL PRIMARY KEY,
    ver BIGINT NOT NULL DEFAULT 0,
    request_id VARCHAR(160) NOT NULL,
    destinatario_kind VARCHAR(100) NOT NULL,
    organization_scope VARCHAR(100),
    unidade_codigo VARCHAR(180) NOT NULL,
    orgao_sigla VARCHAR(120) NOT NULL,
    representante_usuario_id BIGINT,
    materialized_affiliation_id VARCHAR(160),
    status_codigo VARCHAR(80) NOT NULL,
    hash_integridade VARCHAR(128) NOT NULL,
    snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_inst_aff_req_id UNIQUE (request_id)
);

CREATE INDEX IF NOT EXISTS idx_inst_aff_req_status
    ON tb_inst_affiliation_request_snapshot (status_codigo, updated_at);
CREATE INDEX IF NOT EXISTS idx_inst_aff_req_unidade
    ON tb_inst_affiliation_request_snapshot (unidade_codigo, updated_at);
CREATE INDEX IF NOT EXISTS idx_inst_aff_req_orgao
    ON tb_inst_affiliation_request_snapshot (orgao_sigla, updated_at);
CREATE INDEX IF NOT EXISTS idx_inst_aff_req_scope
    ON tb_inst_affiliation_request_snapshot (organization_scope, updated_at);
CREATE INDEX IF NOT EXISTS idx_inst_aff_req_rep_user
    ON tb_inst_affiliation_request_snapshot (representante_usuario_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_inst_aff_req_materialized
    ON tb_inst_affiliation_request_snapshot (materialized_affiliation_id, updated_at);

CREATE TABLE IF NOT EXISTS tb_processual_read_model_projection (
    id BIGSERIAL PRIMARY KEY,
    domain VARCHAR(120) NOT NULL,
    materialization_key VARCHAR(240) NOT NULL,
    scope_key VARCHAR(240),
    aggregate_type VARCHAR(120),
    aggregate_id VARCHAR(160),
    tribunal_code VARCHAR(80),
    ramo_code VARCHAR(80),
    projection_version BIGINT NOT NULL,
    last_event_type VARCHAR(160),
    source VARCHAR(80),
    payload_hash VARCHAR(96),
    payload_json TEXT,
    status VARCHAR(40) NOT NULL,
    freshness_at TIMESTAMP,
    last_materialized_at TIMESTAMP,
    last_recomposition_requested_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_prm_projection_domain_key
    ON tb_processual_read_model_projection (domain, materialization_key);
CREATE INDEX IF NOT EXISTS idx_prm_projection_scope
    ON tb_processual_read_model_projection (domain, tribunal_code, ramo_code, scope_key);
CREATE INDEX IF NOT EXISTS idx_prm_projection_status
    ON tb_processual_read_model_projection (status);
CREATE INDEX IF NOT EXISTS idx_prm_projection_updated
    ON tb_processual_read_model_projection (updated_at);

CREATE TABLE IF NOT EXISTS tb_processual_read_model_recomp_job (
    id BIGSERIAL PRIMARY KEY,
    domain VARCHAR(120) NOT NULL,
    tribunal_code VARCHAR(80),
    ramo_code VARCHAR(80),
    scope_key VARCHAR(240),
    status VARCHAR(40) NOT NULL,
    requested_by VARCHAR(120),
    reason TEXT,
    not_before_at TIMESTAMP,
    attempt_count INTEGER NOT NULL,
    last_error TEXT,
    last_claimed_at TIMESTAMP,
    last_completed_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prm_recomp_status_not_before
    ON tb_processual_read_model_recomp_job (status, not_before_at);
CREATE INDEX IF NOT EXISTS idx_prm_recomp_scope
    ON tb_processual_read_model_recomp_job (domain, tribunal_code, ramo_code, scope_key);
CREATE INDEX IF NOT EXISTS idx_prm_recomp_updated
    ON tb_processual_read_model_recomp_job (updated_at);

CREATE TABLE IF NOT EXISTS tb_processual_read_model_trail (
    id BIGSERIAL PRIMARY KEY,
    projection_domain VARCHAR(120) NOT NULL,
    projection_key VARCHAR(240) NOT NULL,
    projection_version BIGINT NOT NULL,
    previous_version BIGINT,
    event_type VARCHAR(160),
    aggregate_type VARCHAR(120),
    aggregate_id VARCHAR(160),
    tribunal_code VARCHAR(80),
    ramo_code VARCHAR(80),
    source VARCHAR(80),
    materialization_hash VARCHAR(96),
    materialization_status VARCHAR(40) NOT NULL,
    payload_snapshot_json TEXT,
    notes TEXT,
    occurred_at TIMESTAMP,
    created_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_prm_trail_domain_key_version
    ON tb_processual_read_model_trail (projection_domain, projection_key, projection_version);
CREATE INDEX IF NOT EXISTS idx_prm_trail_scope
    ON tb_processual_read_model_trail (projection_domain, tribunal_code, ramo_code);
CREATE INDEX IF NOT EXISTS idx_prm_trail_created
    ON tb_processual_read_model_trail (created_at);
