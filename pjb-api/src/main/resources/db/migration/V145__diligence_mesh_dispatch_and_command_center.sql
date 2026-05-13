CREATE TABLE IF NOT EXISTS tb_diligencia_operador_malha_institucional_dispatch (
    id BIGSERIAL PRIMARY KEY,
    operator_user_id BIGINT NOT NULL,
    operator_tipo_usuario VARCHAR(80) NOT NULL,
    canal VARCHAR(40) NOT NULL,
    diligence_reference VARCHAR(120) NOT NULL,
    processo_id BIGINT NOT NULL,
    processo_numero VARCHAR(32),
    work_item_id BIGINT,
    annexation_id BIGINT NOT NULL,
    juntada_id BIGINT,
    pacote_documento_id UUID,
    outbox_event_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(120) NOT NULL,
    routing_key VARCHAR(180) NOT NULL,
    external_system_code VARCHAR(40) NOT NULL,
    destination_box VARCHAR(160) NOT NULL,
    mesh_org_key VARCHAR(80) NOT NULL,
    mesh_unit_key VARCHAR(120) NOT NULL,
    dispatch_status VARCHAR(40) NOT NULL,
    replay_token VARCHAR(64) NOT NULL UNIQUE,
    chain_idempotency_key VARCHAR(64) NOT NULL,
    request_hash_sha256 VARCHAR(64) NOT NULL,
    payload_digest_sha256 VARCHAR(64) NOT NULL,
    payload_signature_hmac_sha256 VARCHAR(64) NOT NULL,
    ack_protocol VARCHAR(120),
    ack_reference VARCHAR(160),
    observacoes VARCHAR(3000),
    delivered_at TIMESTAMP,
    acknowledged_at TIMESTAMP,
    request_id VARCHAR(80),
    created_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_diligencia_mesh_dispatch_chain
    ON tb_diligencia_operador_malha_institucional_dispatch(operator_user_id, canal, diligence_reference, chain_idempotency_key);

CREATE INDEX IF NOT EXISTS idx_diligencia_mesh_dispatch_user_ref
    ON tb_diligencia_operador_malha_institucional_dispatch(operator_user_id, canal, diligence_reference, created_at);

CREATE INDEX IF NOT EXISTS idx_diligencia_mesh_dispatch_processo
    ON tb_diligencia_operador_malha_institucional_dispatch(processo_id, created_at);

CREATE INDEX IF NOT EXISTS idx_diligencia_mesh_dispatch_annex
    ON tb_diligencia_operador_malha_institucional_dispatch(annexation_id, created_at);

CREATE INDEX IF NOT EXISTS idx_diligencia_mesh_dispatch_org_unit
    ON tb_diligencia_operador_malha_institucional_dispatch(mesh_org_key, mesh_unit_key, created_at);
