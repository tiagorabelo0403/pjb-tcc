CREATE TABLE IF NOT EXISTS tb_tema_repercussao_geral (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(80) NOT NULL UNIQUE,
    modalidade VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    ementa TEXT NOT NULL,
    tese_firmada TEXT,
    efeitos_processuais TEXT,
    fundamentos_resumo TEXT,
    processos_sobrestados INTEGER NOT NULL DEFAULT 0,
    processos_aplicados INTEGER NOT NULL DEFAULT 0,
    score_corte INTEGER,
    processos_relacionados_json TEXT,
    leading_case_processo_id BIGINT REFERENCES tb_processo(id),
    relator_id BIGINT REFERENCES tb_usuario(id),
    reconhecido_em TIMESTAMPTZ,
    julgado_em TIMESTAMPTZ,
    aplicado_em TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_tema_rg_status
    ON tb_tema_repercussao_geral (status);

CREATE INDEX IF NOT EXISTS idx_tema_rg_modalidade
    ON tb_tema_repercussao_geral (modalidade);

CREATE INDEX IF NOT EXISTS idx_tema_rg_leading_case
    ON tb_tema_repercussao_geral (leading_case_processo_id);
