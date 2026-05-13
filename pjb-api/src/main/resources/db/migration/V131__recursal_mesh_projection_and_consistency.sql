CREATE TABLE IF NOT EXISTS tb_recursal_mesh_process_projection (
    recurso_id VARCHAR(160) PRIMARY KEY,
    processo_id BIGINT,
    numero_processo VARCHAR(50),
    species_code VARCHAR(30) NOT NULL,
    profile_name VARCHAR(120) NOT NULL,
    current_state VARCHAR(40) NOT NULL,
    tribunal_atual VARCHAR(20) NOT NULL,
    tribunal_detalhado_atual VARCHAR(20) NOT NULL,
    instancia_atual VARCHAR(20) NOT NULL,
    autoridade_atual VARCHAR(30) NOT NULL,
    last_event VARCHAR(40),
    current_revision INTEGER NOT NULL DEFAULT 0,
    total_transitions INTEGER NOT NULL DEFAULT 0,
    iteracoes_embargos INTEGER NOT NULL DEFAULT 0,
    transitado_em_julgado BOOLEAN NOT NULL DEFAULT FALSE,
    last_actor VARCHAR(160),
    last_transition_at TIMESTAMP,
    snapshot_json TEXT NOT NULL,
    route_plan_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_recursal_mesh_projection_processo FOREIGN KEY (processo_id) REFERENCES tb_processo(id)
);

CREATE INDEX IF NOT EXISTS idx_recursal_mesh_projection_processo ON tb_recursal_mesh_process_projection(processo_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_recursal_mesh_projection_state ON tb_recursal_mesh_process_projection(current_state, tribunal_atual, tribunal_detalhado_atual);
