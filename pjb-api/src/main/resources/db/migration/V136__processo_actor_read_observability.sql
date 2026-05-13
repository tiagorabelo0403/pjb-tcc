CREATE TABLE IF NOT EXISTS tb_processo_leitura_ator (
    id BIGSERIAL PRIMARY KEY,
    processo_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    actor_role VARCHAR(60) NOT NULL,
    actor_cluster VARCHAR(60) NOT NULL,
    actor_display_name VARCHAR(180) NOT NULL,
    first_read_at TIMESTAMP NOT NULL,
    last_read_at TIMESTAMP NOT NULL,
    read_count BIGINT NOT NULL DEFAULT 0,
    last_channel VARCHAR(80) NULL,
    last_request_id VARCHAR(120) NULL,
    last_justificativa VARCHAR(500) NULL,
    last_step_up_satisfied BOOLEAN NOT NULL DEFAULT FALSE,
    last_party_signal_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_proc_leitura_processo FOREIGN KEY (processo_id) REFERENCES tb_processo(id) ON DELETE CASCADE,
    CONSTRAINT fk_proc_leitura_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id) ON DELETE CASCADE,
    CONSTRAINT uk_proc_leitura_ator UNIQUE (processo_id, usuario_id)
);

CREATE INDEX IF NOT EXISTS idx_proc_leitura_proc_last ON tb_processo_leitura_ator(processo_id, last_read_at DESC);
CREATE INDEX IF NOT EXISTS idx_proc_leitura_user_last ON tb_processo_leitura_ator(usuario_id, last_read_at DESC);
CREATE INDEX IF NOT EXISTS idx_proc_leitura_cluster_last ON tb_processo_leitura_ator(processo_id, actor_cluster, last_read_at DESC);
