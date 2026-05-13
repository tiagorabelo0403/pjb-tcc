-- Permite que o mesmo CPF (hash) exista para múltiplos advogados.
-- Unicidade passa a ser por (advogado_id, cpf_hash).

ALTER TABLE adv_clientes DROP CONSTRAINT IF EXISTS adv_clientes_cpf_hash_key;

DROP INDEX IF EXISTS idx_cliente_cpf_hash;
DROP INDEX IF EXISTS idx_cliente_advogado_cpf_hash;

CREATE UNIQUE INDEX IF NOT EXISTS idx_cliente_advogado_cpf_hash ON adv_clientes (advogado_id, cpf_hash);
CREATE INDEX IF NOT EXISTS idx_cliente_cpf_hash ON adv_clientes (cpf_hash);
