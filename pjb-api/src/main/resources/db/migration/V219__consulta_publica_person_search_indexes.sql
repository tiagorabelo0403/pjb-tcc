CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_tb_processo_public_nome_autor_trgm
    ON tb_processo USING gin (lower(parte_autora_nome) gin_trgm_ops)
    WHERE nivel_sigilo = 'PUBLICO' AND parte_autora_nome IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_tb_processo_public_nome_reu_trgm
    ON tb_processo USING gin (lower(parte_reu_nome) gin_trgm_ops)
    WHERE nivel_sigilo = 'PUBLICO' AND parte_reu_nome IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_tb_processo_public_numero_unificado_trgm
    ON tb_processo USING gin (lower(numero_unificado) gin_trgm_ops)
    WHERE nivel_sigilo = 'PUBLICO' AND numero_unificado IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_tb_processo_public_numero_processo_trgm
    ON tb_processo USING gin (lower(numero_processo) gin_trgm_ops)
    WHERE nivel_sigilo = 'PUBLICO' AND numero_processo IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_tb_processo_public_regiao_movimentacao
    ON tb_processo (nivel_sigilo, uf, comarca, vara, data_ultima_movimentacao DESC, id DESC);
