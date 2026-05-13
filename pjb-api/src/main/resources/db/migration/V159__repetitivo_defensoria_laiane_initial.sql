CREATE TABLE IF NOT EXISTS tb_tema_recurso_repetitivo (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(80) NOT NULL UNIQUE,
    tribunal_sigla VARCHAR(30),
    status VARCHAR(40) NOT NULL,
    ementa TEXT NOT NULL,
    tese_firmada TEXT,
    fundamentos_resumo TEXT,
    criterio_afetacao TEXT,
    processos_sobrestados INTEGER NOT NULL DEFAULT 0,
    processos_aplicados INTEGER NOT NULL DEFAULT 0,
    processos_relacionados_json TEXT,
    recurso_representativo_processo_id BIGINT REFERENCES tb_processo(id),
    relator_id BIGINT REFERENCES tb_usuario(id),
    afetado_em TIMESTAMPTZ,
    julgado_em TIMESTAMPTZ,
    aplicado_em TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_tema_rr_status
    ON tb_tema_recurso_repetitivo (status);

CREATE INDEX IF NOT EXISTS idx_tema_rr_tribunal
    ON tb_tema_recurso_repetitivo (tribunal_sigla);

CREATE TABLE IF NOT EXISTS tb_defensoria_vulnerabilidade_caso (
    id BIGSERIAL PRIMARY KEY,
    defensor_id BIGINT NOT NULL REFERENCES tb_usuario(id),
    processo_id BIGINT REFERENCES tb_processo(id),
    assistido_nome VARCHAR(180) NOT NULL,
    documento_identificador VARCHAR(40),
    score_vulnerabilidade INTEGER NOT NULL,
    prioridade_faixa VARCHAR(30) NOT NULL,
    hipervulnerabilidades_json TEXT,
    sinais_risco_json TEXT,
    observacoes TEXT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_def_vuln_defensor
    ON tb_defensoria_vulnerabilidade_caso (defensor_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_def_vuln_status
    ON tb_defensoria_vulnerabilidade_caso (status, prioridade_faixa);

CREATE TABLE IF NOT EXISTS tb_laiane_peticao_inicial_draft (
    id BIGSERIAL PRIMARY KEY,
    solicitante_id BIGINT NOT NULL REFERENCES tb_usuario(id),
    processo_id BIGINT REFERENCES tb_processo(id),
    titulo_caso VARCHAR(180) NOT NULL,
    ramo_direito VARCHAR(40),
    rito_sugerido VARCHAR(80),
    classe_sugerida VARCHAR(120),
    urgencia_score INTEGER NOT NULL DEFAULT 0,
    readiness_score INTEGER NOT NULL DEFAULT 0,
    fatos_json TEXT,
    pedidos_json TEXT,
    fundamentos_json TEXT,
    provas_json TEXT,
    checklist_json TEXT,
    minuta_inicial TEXT NOT NULL,
    hash_integridade VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_laiane_initial_user
    ON tb_laiane_peticao_inicial_draft (solicitante_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_laiane_initial_status
    ON tb_laiane_peticao_inicial_draft (status, ramo_direito);
