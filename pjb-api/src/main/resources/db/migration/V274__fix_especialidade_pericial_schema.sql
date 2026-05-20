-- Entidade EspecialidadePericial redesenhada: nome->codigo, novo campo descricao
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name='tb_especialidade_pericial' AND column_name='nome') THEN
        ALTER TABLE tb_especialidade_pericial RENAME COLUMN nome TO codigo;
    END IF;
END$$;

ALTER TABLE tb_especialidade_pericial
    ALTER COLUMN codigo TYPE varchar(50);

ALTER TABLE tb_especialidade_pericial
    ADD COLUMN IF NOT EXISTS descricao varchar(150) NOT NULL DEFAULT '';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name='tb_especialidade_pericial' AND column_name='area'
                 AND character_maximum_length IS NOT NULL AND character_maximum_length > 50) THEN
        ALTER TABLE tb_especialidade_pericial ALTER COLUMN area TYPE varchar(50);
    END IF;
END$$;

ALTER TABLE tb_especialidade_pericial
    ALTER COLUMN area SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'tb_especialidade_pericial'::regclass
          AND contype = 'u'
          AND conname LIKE '%codigo%'
    ) THEN
        ALTER TABLE tb_especialidade_pericial
            ADD CONSTRAINT uk_especialidade_codigo UNIQUE (codigo);
    END IF;
END$$;
