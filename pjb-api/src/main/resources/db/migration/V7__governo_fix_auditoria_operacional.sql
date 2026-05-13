-- PJB Governance Patch (V7)
-- Objetivo: manter compatibilidade entre schema e modelo JPA da auditoria operacional.
--
-- - Flyway V6 criou tb_auditoria_evento com a coluna data_hora.
-- - Em alguns ambientes (ddl-auto legado), pode existir criado_em.
--
-- Estratégia "Estado": não quebrar ambientes divergentes; adicionar colunas ausentes e
-- migrar dados quando possível.

-- 1) Campo de origem (IP/host/proxy) para trilha de auditoria.
ALTER TABLE tb_auditoria_evento
    ADD COLUMN IF NOT EXISTS origem VARCHAR(120);

-- 2) Garante presença de data_hora (coluna canônica usada pelo JPA).
ALTER TABLE tb_auditoria_evento
    ADD COLUMN IF NOT EXISTS data_hora TIMESTAMP NOT NULL DEFAULT NOW();

-- 3) Se existir coluna criado_em (legado), tenta copiar valores para data_hora (quando vazio).
--    Usamos bloco condicional compatível com PostgreSQL.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'tb_auditoria_evento'
          AND column_name = 'criado_em'
    ) THEN
        EXECUTE 'UPDATE tb_auditoria_evento SET data_hora = criado_em WHERE data_hora IS NULL';
    END IF;
END $$;
