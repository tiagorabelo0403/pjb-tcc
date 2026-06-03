CREATE TABLE tb_protocolo_completude_outbox (
    id              UUID NOT NULL,
    protocolo_id    BIGINT NOT NULL,
    tipo            VARCHAR(60) NOT NULL,
    payload         JSONB NOT NULL,
    processado      BOOLEAN NOT NULL DEFAULT FALSE,
    tentativas      INTEGER NOT NULL DEFAULT 0,
    criado_em       TIMESTAMPTZ NOT NULL,
    processado_em   TIMESTAMPTZ,
    CONSTRAINT pk_protocolo_completude_outbox PRIMARY KEY (id)
);

CREATE INDEX idx_completude_outbox_pendente
    ON tb_protocolo_completude_outbox (criado_em)
    WHERE processado = FALSE;
