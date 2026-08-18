-- Functional indexes for UPPER(tribunal) used in JPQL WHERE clauses
-- Replaces COALESCE-wrapped UPPER() patterns that prevented index usage

CREATE INDEX IF NOT EXISTS idx_tb_processo_tribunal_upper
    ON tb_processo (UPPER(tribunal))
    WHERE tribunal IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_jurisdicoes_codigo_upper
    ON jurisdicoes (UPPER(codigo))
    WHERE codigo IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_jurisdicoes_sigla_upper
    ON jurisdicoes (UPPER(sigla))
    WHERE sigla IS NOT NULL;
