CREATE TABLE IF NOT EXISTS tb_work_item (
    id BIGSERIAL PRIMARY KEY,
    processo_id BIGINT NOT NULL,
    fase_origem VARCHAR(60),
    template_code VARCHAR(120),
    type VARCHAR(40),
    titulo VARCHAR(220) NOT NULL,
    descricao TEXT,
    assigned_role VARCHAR(60),
    assigned_user_id BIGINT,
    status VARCHAR(30) NOT NULL,
    prioridade INTEGER,
    blocking BOOLEAN NOT NULL DEFAULT FALSE,
    due_at TIMESTAMP,
    uf VARCHAR(2),
    comarca VARCHAR(120),
    base_legal TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_workitem_processo'
    ) THEN
        ALTER TABLE tb_work_item
            ADD CONSTRAINT fk_workitem_processo FOREIGN KEY (processo_id)
                REFERENCES tb_processo(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_workitem_user'
    ) THEN
        ALTER TABLE tb_work_item
            ADD CONSTRAINT fk_workitem_user FOREIGN KEY (assigned_user_id)
                REFERENCES tb_usuario(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_workitem_status ON tb_work_item(status);
CREATE INDEX IF NOT EXISTS idx_workitem_role ON tb_work_item(assigned_role, status);
CREATE INDEX IF NOT EXISTS idx_workitem_processo ON tb_work_item(processo_id, status);
CREATE INDEX IF NOT EXISTS idx_workitem_template ON tb_work_item(template_code);

CREATE TABLE IF NOT EXISTS tb_movimentacao_processual (
    id BIGSERIAL PRIMARY KEY,
    processo_id BIGINT NOT NULL,
    fase_de VARCHAR(60),
    fase_para VARCHAR(60),
    descricao TEXT,
    ator_user_id BIGINT,
    data_movimentacao TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_mov_processo'
    ) THEN
        ALTER TABLE tb_movimentacao_processual
            ADD CONSTRAINT fk_mov_processo FOREIGN KEY (processo_id)
                REFERENCES tb_processo(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_mov_user'
    ) THEN
        ALTER TABLE tb_movimentacao_processual
            ADD CONSTRAINT fk_mov_user FOREIGN KEY (ator_user_id)
                REFERENCES tb_usuario(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_mov_processo ON tb_movimentacao_processual(processo_id, data_movimentacao);

CREATE TABLE IF NOT EXISTS tb_precedente (
    id BIGSERIAL PRIMARY KEY,
    fonte VARCHAR(30) NOT NULL,
    tipo VARCHAR(60) NOT NULL,
    identificador VARCHAR(80),
    titulo VARCHAR(260),
    tese TEXT,
    ementa_resumo TEXT,
    url_referencia VARCHAR(600),
    data_publicacao DATE,
    ramo_sugerido VARCHAR(60),
    rito_sugerido VARCHAR(60),
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prec_fonte_tipo ON tb_precedente(fonte, tipo);
CREATE INDEX IF NOT EXISTS idx_prec_ident ON tb_precedente(identificador);
CREATE INDEX IF NOT EXISTS idx_prec_data ON tb_precedente(data_publicacao);
