-- Workspace / Escritório scoping para advocacia.
--
-- Objetivo:
--  - permitir atuação “independente” (equipe_id NULL) ou “pelo escritório” (equipe_id != NULL)
--  - habilitar o Hibernate Filter (filtroEquipe) com condição por (usuarioIdParam, equipeIdParam)

CREATE TABLE IF NOT EXISTS equipes (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_equipe_nome UNIQUE (nome)
);

CREATE TABLE IF NOT EXISTS membros_equipe (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    equipe_id BIGINT NOT NULL,
    papel VARCHAR(40) NOT NULL,
    cargo VARCHAR(100),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_admissao DATE,
    data_entrada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultima_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_membro_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id),
    CONSTRAINT fk_membro_equipe FOREIGN KEY (equipe_id) REFERENCES equipes(id),
    CONSTRAINT uk_usuario_equipe UNIQUE (usuario_id, equipe_id)
);

CREATE INDEX IF NOT EXISTS idx_membro_equipe_usuario ON membros_equipe(usuario_id);
CREATE INDEX IF NOT EXISTS idx_membro_equipe_equipe ON membros_equipe(equipe_id);

-- tb_processo
ALTER TABLE tb_processo
    ADD COLUMN IF NOT EXISTS equipe_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_processo_equipe'
          AND table_name = 'tb_processo'
    ) THEN
        ALTER TABLE tb_processo
            ADD CONSTRAINT fk_processo_equipe
                FOREIGN KEY (equipe_id) REFERENCES equipes(id)
                ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_processo_equipe_id ON tb_processo(equipe_id);

DO $$
BEGIN
    IF to_regclass('public.adv_clientes') IS NOT NULL THEN
        EXECUTE 'ALTER TABLE adv_clientes ADD COLUMN IF NOT EXISTS equipe_id BIGINT';

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE constraint_name = 'fk_adv_cliente_equipe'
              AND table_name = 'adv_clientes'
        ) THEN
            EXECUTE 'ALTER TABLE adv_clientes ADD CONSTRAINT fk_adv_cliente_equipe FOREIGN KEY (equipe_id) REFERENCES equipes(id) ON DELETE SET NULL';
        END IF;

        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_adv_cliente_equipe_id ON adv_clientes(equipe_id)';
    END IF;
END $$;
