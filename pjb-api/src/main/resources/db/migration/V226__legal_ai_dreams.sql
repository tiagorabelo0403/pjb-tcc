CREATE TABLE IF NOT EXISTS dream (
    id                      UUID        NOT NULL,
    anthropic_dream_id      VARCHAR(255),
    status                  VARCHAR(50) NOT NULL,
    input_store_id          UUID        NOT NULL REFERENCES memory_store_ref(id),
    output_store_id         UUID        REFERENCES memory_store_ref(id),
    session_ids_json        TEXT,
    instrucoes              VARCHAR(4096),
    modelo                  VARCHAR(100),
    criado_em               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    encerrado_em            TIMESTAMPTZ,
    input_tokens            BIGINT,
    output_tokens           BIGINT,
    erro                    TEXT,
    anthropic_output_store_id VARCHAR(255),
    CONSTRAINT pk_dream PRIMARY KEY (id),
    CONSTRAINT uq_dream_anthropic_id UNIQUE (anthropic_dream_id)
);

CREATE INDEX IF NOT EXISTS idx_dream_status ON dream (status);
CREATE INDEX IF NOT EXISTS idx_dream_input_store ON dream (input_store_id);
CREATE INDEX IF NOT EXISTS idx_dream_criado_em ON dream (criado_em DESC);

CREATE TABLE IF NOT EXISTS dream_outbox (
    id              UUID        NOT NULL,
    dream_id        UUID        NOT NULL REFERENCES dream(id),
    payload         JSONB       NOT NULL,
    processado      BOOLEAN     NOT NULL DEFAULT FALSE,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processado_em   TIMESTAMPTZ,
    CONSTRAINT pk_dream_outbox PRIMARY KEY (id),
    CONSTRAINT fk_dream_outbox_dream FOREIGN KEY (dream_id) REFERENCES dream(id)
);

CREATE INDEX IF NOT EXISTS idx_dream_outbox_nao_processado
    ON dream_outbox (criado_em ASC)
    WHERE processado = FALSE;
