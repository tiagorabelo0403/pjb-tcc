-- V33: Blindagem de produção (Kernel Recursal / Timeline)
-- 1) Timestamps em UTC (TIMESTAMP WITH TIME ZONE / timestamptz)
-- 2) Optimistic locking (@Version) nos agregados do grafo recursal
--
-- Observação:
-- - Para converter TIMESTAMP (sem tz) -> TIMESTAMPTZ, assumimos que o valor existente já está em UTC.
--   Por isso usamos "AT TIME ZONE 'UTC'".

-- ===============================
-- 1) Optimistic Locking
-- ===============================
ALTER TABLE tb_case_file
    ADD COLUMN IF NOT EXISTS ver BIGINT NOT NULL DEFAULT 0;

ALTER TABLE tb_case_proceeding
    ADD COLUMN IF NOT EXISTS ver BIGINT NOT NULL DEFAULT 0;

-- ===============================
-- 2) Conversões TIMESTAMP -> TIMESTAMPTZ (UTC)
-- ===============================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'tb_work_item'
          AND column_name = 'due_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE tb_work_item
            ALTER COLUMN due_at TYPE TIMESTAMPTZ
            USING due_at AT TIME ZONE 'UTC';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'tb_work_item'
          AND column_name = 'created_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE tb_work_item
            ALTER COLUMN created_at TYPE TIMESTAMPTZ
            USING created_at AT TIME ZONE 'UTC';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'tb_work_item'
          AND column_name = 'updated_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE tb_work_item
            ALTER COLUMN updated_at TYPE TIMESTAMPTZ
            USING updated_at AT TIME ZONE 'UTC';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'tb_movimentacao_processual'
          AND column_name = 'data_movimentacao'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE tb_movimentacao_processual
            ALTER COLUMN data_movimentacao TYPE TIMESTAMPTZ
            USING data_movimentacao AT TIME ZONE 'UTC';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'pjb_calendario_forense'
          AND column_name = 'created_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE pjb_calendario_forense
            ALTER COLUMN created_at TYPE TIMESTAMPTZ
            USING created_at AT TIME ZONE 'UTC';
    END IF;
END $$;

-- Kernel Processual: event store
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'tb_processo_event'
          AND column_name = 'created_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE tb_processo_event
            ALTER COLUMN created_at TYPE TIMESTAMPTZ
            USING created_at AT TIME ZONE 'UTC';
    END IF;
END $$;

-- Kernel Recursal: grafo
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'tb_case_file'
          AND column_name = 'created_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE tb_case_file
            ALTER COLUMN created_at TYPE TIMESTAMPTZ
            USING created_at AT TIME ZONE 'UTC';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'tb_case_file'
          AND column_name = 'updated_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE tb_case_file
            ALTER COLUMN updated_at TYPE TIMESTAMPTZ
            USING updated_at AT TIME ZONE 'UTC';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'tb_case_proceeding'
          AND column_name = 'created_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE tb_case_proceeding
            ALTER COLUMN created_at TYPE TIMESTAMPTZ
            USING created_at AT TIME ZONE 'UTC';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'tb_case_proceeding'
          AND column_name = 'updated_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE tb_case_proceeding
            ALTER COLUMN updated_at TYPE TIMESTAMPTZ
            USING updated_at AT TIME ZONE 'UTC';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'tb_case_edge'
          AND column_name = 'created_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE tb_case_edge
            ALTER COLUMN created_at TYPE TIMESTAMPTZ
            USING created_at AT TIME ZONE 'UTC';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'tb_case_file_event'
          AND column_name = 'created_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE tb_case_file_event
            ALTER COLUMN created_at TYPE TIMESTAMPTZ
            USING created_at AT TIME ZONE 'UTC';
    END IF;
END $$;
