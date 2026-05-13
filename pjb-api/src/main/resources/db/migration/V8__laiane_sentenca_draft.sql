-- LAIANE: armazenamento robusto de minutas de SENTENÇA (rascunho/publicada)
-- Objetivo: permitir geração idempotente, auditoria e recuperação rápida (cacheável)

CREATE TABLE IF NOT EXISTS tb_laiane_sentenca_draft (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE,
    processo_id     BIGINT NOT NULL,
    criado_por_usuario_id BIGINT,

    -- Status da minuta
    status          VARCHAR(24) NOT NULL, -- DRAFT | PUBLISHED | ARCHIVED

    -- Hash de idempotência: evita gerar duplicado para mesmo contexto/entrada
    input_hash      VARCHAR(64) NOT NULL,

    -- Conteúdo (markdown) e contexto usado para compor a sentença
    draft_markdown  TEXT NOT NULL,
    context_json    TEXT,

    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,
    published_at    TIMESTAMP,

    CONSTRAINT fk_laiane_sentenca_processo FOREIGN KEY (processo_id) REFERENCES tb_processo(id),
    CONSTRAINT fk_laiane_sentenca_usuario FOREIGN KEY (criado_por_usuario_id) REFERENCES tb_usuario(id)
);

CREATE INDEX IF NOT EXISTS idx_laiane_sentenca_processo_ts ON tb_laiane_sentenca_draft(processo_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_laiane_sentenca_processo_status ON tb_laiane_sentenca_draft(processo_id, status);
CREATE INDEX IF NOT EXISTS idx_laiane_sentenca_hash ON tb_laiane_sentenca_draft(input_hash);
