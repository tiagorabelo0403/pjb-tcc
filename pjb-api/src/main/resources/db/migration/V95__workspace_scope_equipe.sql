-- Workspace / Escritório scoping para advocacia.
--
-- Objetivo:
--  - permitir atuação “independente” (equipe_id NULL) ou “pelo escritório” (equipe_id != NULL)
--  - habilitar o Hibernate Filter (filtroEquipe) com condição por (usuarioIdParam, equipeIdParam)

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

-- adv_clientes (clientes do advogado / do escritório)
ALTER TABLE adv_clientes
    ADD COLUMN IF NOT EXISTS equipe_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_adv_cliente_equipe'
          AND table_name = 'adv_clientes'
    ) THEN
        ALTER TABLE adv_clientes
            ADD CONSTRAINT fk_adv_cliente_equipe
                FOREIGN KEY (equipe_id) REFERENCES equipes(id)
                ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_adv_cliente_equipe_id ON adv_clientes(equipe_id);
