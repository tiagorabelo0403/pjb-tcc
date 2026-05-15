CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tb_processo_assunto_trgm
    ON tb_processo USING GIN (lower(assunto) gin_trgm_ops)
    WHERE assunto IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tb_processo_parte_autora_trgm
    ON tb_processo USING GIN (lower(parte_autora_nome) gin_trgm_ops)
    WHERE parte_autora_nome IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tb_processo_parte_reu_trgm
    ON tb_processo USING GIN (lower(parte_reu_nome) gin_trgm_ops)
    WHERE parte_reu_nome IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tb_precedente_titulo_trgm
    ON tb_precedente USING GIN (lower(titulo) gin_trgm_ops)
    WHERE titulo IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tb_precedente_tese_trgm
    ON tb_precedente USING GIN (lower(tese) gin_trgm_ops)
    WHERE tese IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_equipes_nome_lower
    ON equipes (lower(nome));

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_jurisdicoes_nome_lower
    ON jurisdicoes (lower(nome));

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_jurisdicoes_sigla_lower
    ON jurisdicoes (lower(sigla))
    WHERE sigla IS NOT NULL;
