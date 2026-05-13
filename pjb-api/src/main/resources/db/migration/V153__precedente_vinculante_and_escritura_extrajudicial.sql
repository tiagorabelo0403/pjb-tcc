CREATE TABLE IF NOT EXISTS tb_tema_precedente_vinculante (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(80) NOT NULL UNIQUE,
    tipo VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    ementa TEXT NOT NULL,
    tese_firmada TEXT,
    efeitos_processuais TEXT,
    abrangencia VARCHAR(80),
    score_corte INTEGER,
    processos_sobrestados INTEGER,
    processos_aplicados INTEGER,
    fundamentos_resumo TEXT,
    leading_case_processo_id BIGINT,
    relator_id BIGINT,
    julgado_em TIMESTAMPTZ,
    aplicado_em TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tema_precedente_processo FOREIGN KEY (leading_case_processo_id) REFERENCES tb_processo (id),
    CONSTRAINT fk_tema_precedente_relator FOREIGN KEY (relator_id) REFERENCES tb_usuario (id)
);

CREATE INDEX IF NOT EXISTS idx_tema_precedente_status ON tb_tema_precedente_vinculante (status);
CREATE INDEX IF NOT EXISTS idx_tema_precedente_tipo ON tb_tema_precedente_vinculante (tipo);

CREATE TABLE IF NOT EXISTS tb_escritura_extrajudicial_registro (
    id BIGSERIAL PRIMARY KEY,
    protocolo VARCHAR(80) NOT NULL UNIQUE,
    tipo VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    ato_resumo TEXT NOT NULL,
    partes_resumo TEXT NOT NULL,
    bens_resumo TEXT,
    valor_declarado NUMERIC(19,2),
    comarca VARCHAR(120),
    uf VARCHAR(2),
    assinatura_hash VARCHAR(128),
    cartorio_responsavel_id BIGINT,
    processo_vinculado_id BIGINT,
    lavrada_em TIMESTAMPTZ,
    vinculada_em TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_escritura_cartorio FOREIGN KEY (cartorio_responsavel_id) REFERENCES tb_usuario (id),
    CONSTRAINT fk_escritura_processo FOREIGN KEY (processo_vinculado_id) REFERENCES tb_processo (id)
);

CREATE INDEX IF NOT EXISTS idx_escritura_tipo_status ON tb_escritura_extrajudicial_registro (tipo, status);
CREATE INDEX IF NOT EXISTS idx_escritura_processo ON tb_escritura_extrajudicial_registro (processo_vinculado_id);
