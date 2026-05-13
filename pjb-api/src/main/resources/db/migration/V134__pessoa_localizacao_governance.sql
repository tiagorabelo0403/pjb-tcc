CREATE TABLE IF NOT EXISTS tb_pessoa_localizacao_consulta (
    id BIGSERIAL PRIMARY KEY,
    correlation_id VARCHAR(80) NOT NULL,
    executor_user_id BIGINT NOT NULL,
    executor_tipo_usuario VARCHAR(80) NOT NULL,
    canal_consulta VARCHAR(40) NOT NULL,
    fundamento VARCHAR(80) NOT NULL,
    processo_id BIGINT NULL,
    mandado_id BIGINT NULL,
    referencia_procedimental VARCHAR(160) NOT NULL,
    finalidade VARCHAR(500) NOT NULL,
    justificativa_operacional VARCHAR(1000) NOT NULL,
    cpf_hash VARCHAR(128) NOT NULL,
    cpf_mascarado VARCHAR(32) NOT NULL,
    possui_contexto_formal BOOLEAN NOT NULL,
    consulta_sem_processo_autorizada BOOLEAN NOT NULL,
    endereco_estrito_solicitado BOOLEAN NOT NULL,
    endereco_estrito_liberado BOOLEAN NOT NULL,
    nivel_exposicao VARCHAR(32) NOT NULL,
    postura_nivel VARCHAR(24) NOT NULL,
    postura_score INTEGER NOT NULL,
    requer_revisao BOOLEAN NOT NULL,
    modo_liberacao VARCHAR(60) NOT NULL,
    fontes_consultadas INTEGER NOT NULL DEFAULT 0,
    enderecos_encontrados INTEGER NOT NULL DEFAULT 0,
    restricoes_encontradas INTEGER NOT NULL DEFAULT 0,
    vinculos_encontrados INTEGER NOT NULL DEFAULT 0,
    alertas_count INTEGER NOT NULL DEFAULT 0,
    sinais_postura VARCHAR(1500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_pessoa_localizacao_correlation UNIQUE (correlation_id),
    CONSTRAINT fk_pessoa_localizacao_executor FOREIGN KEY (executor_user_id) REFERENCES tb_usuario(id)
);

CREATE INDEX IF NOT EXISTS idx_pessoa_localizacao_executor_created ON tb_pessoa_localizacao_consulta(executor_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_pessoa_localizacao_canal_created ON tb_pessoa_localizacao_consulta(canal_consulta, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_pessoa_localizacao_ref_proc ON tb_pessoa_localizacao_consulta(referencia_procedimental);
CREATE INDEX IF NOT EXISTS idx_pessoa_localizacao_postura ON tb_pessoa_localizacao_consulta(postura_nivel, requer_revisao, created_at DESC);
