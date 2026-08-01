-- V307 - Ativa extensao pgvector e cria store minimo para o VectorSearchService.
-- Nao invalida o modo disabled/mock: a extensao so e usada quando
-- pjb.ai.vector.mode=pgvector estiver ligada (padrao segue disabled).
--
-- Dimensao 1536: bate com text-embedding-3-small e text-embedding-ada-002 (OpenAI),
-- os dois modelos ja suportados via spring-ai-openai (application-ai.yml / application-integrations.yml).
-- Se um dia trocar para 3-large (3072) ou um modelo maior, criar uma tabela nova numa
-- migration posterior — pgvector nao aceita ALTER de dimensao numa coluna existente.
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS pjb_ai_vector_document (
    id           BIGSERIAL PRIMARY KEY,
    doc_id       VARCHAR(120) NOT NULL,
    titulo       VARCHAR(500) NOT NULL,
    ramo         VARCHAR(80),
    conteudo     TEXT         NOT NULL,
    embedding    vector(1536) NOT NULL,
    metadata     JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_pjb_ai_vector_document_doc_id UNIQUE (doc_id)
);

-- Indice HNSW com operator class de cosine distance (<=>): batendo com o padrao mais
-- comum de RAG e com o calculo de score do adapter (score = 1 - cosine_distance).
-- m=16 e ef_construction=64 sao os defaults recomendados; ajustar quando o dataset
-- crescer o suficiente para exigir tuning real.
CREATE INDEX IF NOT EXISTS idx_pjb_ai_vector_document_embedding_hnsw
    ON pjb_ai_vector_document
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- Indice GIN sobre metadata permite filtro por chaves arbitrarias (ramo/rito/tribunal/etc.)
-- sem exigir coluna dedicada para cada filtro futuro.
CREATE INDEX IF NOT EXISTS idx_pjb_ai_vector_document_metadata_gin
    ON pjb_ai_vector_document
    USING gin (metadata jsonb_path_ops);
