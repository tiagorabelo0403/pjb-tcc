-- PJB v26 - Evento Institucional (OAB/UF) + Providências

CREATE TABLE IF NOT EXISTS evento_institucional (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(60) NOT NULL,
    status VARCHAR(40) NOT NULL,
    uf VARCHAR(2) NOT NULL,
    tribunal VARCHAR(40),
    orgao VARCHAR(120),
    processo_id BIGINT,
    numero_processo VARCHAR(40),
    severidade INT NOT NULL DEFAULT 3,
    resumo VARCHAR(240) NOT NULL,
    detalhes TEXT,
    criado_por_usuario_id BIGINT,
    criado_por VARCHAR(160),
    criado_em TIMESTAMPTZ,
    atualizado_em TIMESTAMPTZ
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_evento_institucional_processo'
    ) THEN
        ALTER TABLE evento_institucional
            ADD CONSTRAINT fk_evento_institucional_processo
                FOREIGN KEY (processo_id) REFERENCES tb_processo(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_evento_institucional_actor'
    ) THEN
        ALTER TABLE evento_institucional
            ADD CONSTRAINT fk_evento_institucional_actor
                FOREIGN KEY (criado_por_usuario_id) REFERENCES tb_usuario(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_evento_institucional_uf ON evento_institucional(uf);
CREATE INDEX IF NOT EXISTS idx_evento_institucional_status ON evento_institucional(status);
CREATE INDEX IF NOT EXISTS idx_evento_institucional_processo ON evento_institucional(processo_id);
CREATE INDEX IF NOT EXISTS idx_evento_institucional_orgao ON evento_institucional(orgao);
CREATE INDEX IF NOT EXISTS idx_evento_institucional_numproc ON evento_institucional(numero_processo);
CREATE INDEX IF NOT EXISTS idx_evento_institucional_created ON evento_institucional(criado_em DESC);


CREATE TABLE IF NOT EXISTS providencia_institucional (
    id BIGSERIAL PRIMARY KEY,
    evento_id BIGINT NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    titulo VARCHAR(240) NOT NULL,
    descricao TEXT,
    criado_por_usuario_id BIGINT,
    criado_por VARCHAR(160),
    criado_em TIMESTAMPTZ
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_providencia_institucional_evento'
    ) THEN
        ALTER TABLE providencia_institucional
            ADD CONSTRAINT fk_providencia_institucional_evento
                FOREIGN KEY (evento_id) REFERENCES evento_institucional(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_providencia_institucional_actor'
    ) THEN
        ALTER TABLE providencia_institucional
            ADD CONSTRAINT fk_providencia_institucional_actor
                FOREIGN KEY (criado_por_usuario_id) REFERENCES tb_usuario(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_providencia_evento ON providencia_institucional(evento_id);
CREATE INDEX IF NOT EXISTS idx_providencia_created ON providencia_institucional(criado_em DESC);
