-- Permite que o mesmo CPF (hash) exista para múltiplos advogados.
-- Unicidade passa a ser por (advogado_id, cpf_hash).

DO $$
BEGIN
    IF to_regclass('public.adv_clientes') IS NOT NULL THEN
        EXECUTE 'ALTER TABLE adv_clientes DROP CONSTRAINT IF EXISTS adv_clientes_cpf_hash_key';
        EXECUTE 'DROP INDEX IF EXISTS idx_cliente_cpf_hash';
        EXECUTE 'DROP INDEX IF EXISTS idx_cliente_advogado_cpf_hash';
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS idx_cliente_advogado_cpf_hash ON adv_clientes (advogado_id, cpf_hash)';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_cliente_cpf_hash ON adv_clientes (cpf_hash)';
    END IF;
END $$;
