CREATE TABLE IF NOT EXISTS tb_inst_delegation_assignment_snapshot (
    id BIGSERIAL PRIMARY KEY,
    ver BIGINT NOT NULL DEFAULT 0,
    assignment_id VARCHAR(160) NOT NULL,
    expedicao_uuid VARCHAR(160) NOT NULL,
    processo_id BIGINT NULL,
    unidade_codigo VARCHAR(160) NOT NULL,
    caixa_codigo VARCHAR(160) NOT NULL,
    status_codigo VARCHAR(80) NOT NULL,
    tipo_fluxo VARCHAR(80) NOT NULL,
    delegado_usuario_id BIGINT NOT NULL,
    hash_integridade VARCHAR(128) NOT NULL,
    snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_inst_delegation_assignment_id UNIQUE (assignment_id)
);

CREATE INDEX IF NOT EXISTS idx_inst_delegation_exp ON tb_inst_delegation_assignment_snapshot (expedicao_uuid, updated_at);
CREATE INDEX IF NOT EXISTS idx_inst_delegation_proc ON tb_inst_delegation_assignment_snapshot (processo_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_inst_delegation_status ON tb_inst_delegation_assignment_snapshot (status_codigo, updated_at);

CREATE TABLE IF NOT EXISTS tb_inst_draft_manifestation_snapshot (
    id BIGSERIAL PRIMARY KEY,
    ver BIGINT NOT NULL DEFAULT 0,
    draft_id VARCHAR(160) NOT NULL,
    expedicao_uuid VARCHAR(160) NOT NULL,
    processo_id BIGINT NULL,
    unidade_codigo VARCHAR(160) NOT NULL,
    caixa_codigo VARCHAR(160) NOT NULL,
    status_codigo VARCHAR(80) NOT NULL,
    aprovador_usuario_id BIGINT NULL,
    hash_integridade VARCHAR(128) NOT NULL,
    snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_inst_draft_id UNIQUE (draft_id)
);

CREATE INDEX IF NOT EXISTS idx_inst_draft_exp ON tb_inst_draft_manifestation_snapshot (expedicao_uuid, updated_at);
CREATE INDEX IF NOT EXISTS idx_inst_draft_proc ON tb_inst_draft_manifestation_snapshot (processo_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_inst_draft_status ON tb_inst_draft_manifestation_snapshot (status_codigo, updated_at);
