CREATE TABLE legal_ai_audit_log (
    id              UUID         NOT NULL,
    acao            VARCHAR(60)  NOT NULL,
    entidade_tipo   VARCHAR(100),
    entidade_id     VARCHAR(255),
    ator_id         VARCHAR(255),
    modelo          VARCHAR(100),
    input_tokens    INTEGER,
    output_tokens   INTEGER,
    detalhes        JSONB,
    criado_em       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_legal_ai_audit_log PRIMARY KEY (id)
);

ALTER TABLE legal_ai_audit_log ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_legal_ai_audit_acao
    ON legal_ai_audit_log (acao, criado_em DESC);

CREATE INDEX idx_legal_ai_audit_entidade
    ON legal_ai_audit_log (entidade_tipo, entidade_id)
    WHERE entidade_id IS NOT NULL;

CREATE INDEX idx_legal_ai_audit_ator
    ON legal_ai_audit_log (ator_id)
    WHERE ator_id IS NOT NULL;
