-- V231: governance tables for institutional judicial coordination (Round 28AH)

CREATE TABLE IF NOT EXISTS pjb_instituicao_judicial (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo              VARCHAR(40)  NOT NULL UNIQUE,
    nome                VARCHAR(255) NOT NULL,
    esfera              VARCHAR(60)  NOT NULL,
    uf                  CHAR(2),
    ativo               BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_instituicao_esfera ON pjb_instituicao_judicial (esfera);
CREATE INDEX IF NOT EXISTS idx_instituicao_uf     ON pjb_instituicao_judicial (uf) WHERE uf IS NOT NULL;

CREATE TABLE IF NOT EXISTS pjb_unidade_judiciaria (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instituicao_id      UUID         NOT NULL REFERENCES pjb_instituicao_judicial (id),
    codigo              VARCHAR(60)  NOT NULL UNIQUE,
    nome                VARCHAR(255) NOT NULL,
    tipo                VARCHAR(60)  NOT NULL,
    comarca             VARCHAR(120),
    ativo               BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_unidade_instituicao ON pjb_unidade_judiciaria (instituicao_id);
