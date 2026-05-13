CREATE TABLE IF NOT EXISTS memory_store_ref (
    id                  UUID        NOT NULL,
    nome                VARCHAR(255) NOT NULL,
    descricao           TEXT,
    anthropic_store_id  VARCHAR(255) NOT NULL,
    modulo_origem       VARCHAR(100),
    sigilo_nivel        VARCHAR(50),
    access_type         VARCHAR(50),
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    arquivado_em        TIMESTAMPTZ,
    CONSTRAINT pk_memory_store_ref PRIMARY KEY (id),
    CONSTRAINT uq_memory_store_ref_anthropic_id UNIQUE (anthropic_store_id)
);

CREATE INDEX IF NOT EXISTS idx_memory_store_ref_modulo_origem
    ON memory_store_ref (modulo_origem)
    WHERE arquivado_em IS NULL;

CREATE INDEX IF NOT EXISTS idx_memory_store_ref_sigilo_nivel
    ON memory_store_ref (sigilo_nivel)
    WHERE arquivado_em IS NULL;

ALTER TABLE memory_store_ref ENABLE ROW LEVEL SECURITY;

CREATE TABLE IF NOT EXISTS memory_entry (
    id              UUID        NOT NULL,
    store_id        UUID        NOT NULL REFERENCES memory_store_ref(id),
    chave           VARCHAR(500) NOT NULL,
    conteudo        TEXT        NOT NULL,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ativo           BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_memory_entry PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_memory_entry_store_ativo
    ON memory_entry (store_id)
    WHERE ativo = TRUE;
