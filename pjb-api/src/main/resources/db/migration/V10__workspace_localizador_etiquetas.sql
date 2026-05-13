-- Workspace: Localizadores (filtros salvos) + Etiquetas (tags) + vínculo Processo↔Etiqueta
-- PJB 2026 (Java 21 / Spring Boot 3.x)

CREATE TABLE IF NOT EXISTS tb_workspace_etiqueta (
    id UUID PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    nome VARCHAR(80) NOT NULL,
    cor_hex VARCHAR(12),
    sistema BOOLEAN DEFAULT FALSE,
    criado_em TIMESTAMP,
    atualizado_em TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_workspace_processo_etiqueta (
    id UUID PRIMARY KEY,
    processo_id BIGINT NOT NULL,
    etiqueta_id UUID NOT NULL,
    atribuido_por BIGINT,
    atribuido_em TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_workspace_localizador (
    id UUID PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    nome VARCHAR(120) NOT NULL,
    descricao VARCHAR(400),
    criterio_json TEXT NOT NULL,
    compartilhado BOOLEAN DEFAULT FALSE,
    criado_em TIMESTAMP,
    atualizado_em TIMESTAMP
);

-- Constraints e FKs
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_workspace_pe_processo') THEN
        ALTER TABLE tb_workspace_processo_etiqueta
            ADD CONSTRAINT fk_workspace_pe_processo
            FOREIGN KEY (processo_id) REFERENCES tb_processo(id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_workspace_pe_etiqueta') THEN
        ALTER TABLE tb_workspace_processo_etiqueta
            ADD CONSTRAINT fk_workspace_pe_etiqueta
            FOREIGN KEY (etiqueta_id) REFERENCES tb_workspace_etiqueta(id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ux_workspace_pe_proc_et') THEN
        ALTER TABLE tb_workspace_processo_etiqueta
            ADD CONSTRAINT ux_workspace_pe_proc_et
            UNIQUE (processo_id, etiqueta_id);
    END IF;
END $$;

-- Índices
CREATE INDEX IF NOT EXISTS idx_workspace_etiqueta_owner ON tb_workspace_etiqueta(owner_user_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_workspace_etiqueta_owner_nome_ci ON tb_workspace_etiqueta(owner_user_id, lower(nome));

CREATE INDEX IF NOT EXISTS idx_workspace_pe_processo ON tb_workspace_processo_etiqueta(processo_id);
CREATE INDEX IF NOT EXISTS idx_workspace_pe_etiqueta ON tb_workspace_processo_etiqueta(etiqueta_id);

CREATE INDEX IF NOT EXISTS idx_workspace_localizador_owner ON tb_workspace_localizador(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_workspace_localizador_compartilhado ON tb_workspace_localizador(compartilhado);
CREATE UNIQUE INDEX IF NOT EXISTS ux_workspace_localizador_owner_nome_ci ON tb_workspace_localizador(owner_user_id, lower(nome));
