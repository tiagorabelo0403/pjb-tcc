CREATE TABLE intimacao_audiencia (
    id                    BIGSERIAL PRIMARY KEY,
    audiencia_id          BIGINT       NOT NULL REFERENCES tb_audiencia(id),
    destinatario_nome     VARCHAR(255) NOT NULL,
    destinatario_tipo     VARCHAR(60)  NOT NULL,
    destinatario_oab      VARCHAR(20),
    destinatario_email    VARCHAR(255),
    canal                 VARCHAR(50)  NOT NULL,
    status                VARCHAR(50)  NOT NULL,
    prazo_ciencia         TIMESTAMP WITH TIME ZONE,
    enviada_em            TIMESTAMP WITH TIME ZONE,
    ciencia_em            TIMESTAMP WITH TIME ZONE,
    criado_em             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_intimacao_audiencia_id ON intimacao_audiencia(audiencia_id);
CREATE INDEX idx_intimacao_status       ON intimacao_audiencia(status);

ALTER TABLE intimacao_audiencia ENABLE ROW LEVEL SECURITY;
