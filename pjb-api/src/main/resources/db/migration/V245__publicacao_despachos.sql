-- V245: publicação de despachos e controle de ciência (Round 28AI)

CREATE TABLE IF NOT EXISTS pjb_publicacao (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processo_id             UUID         NOT NULL,
    documento_id            UUID         NOT NULL,
    tipo_ato                VARCHAR(80)  NOT NULL,
    meio                    VARCHAR(60)  NOT NULL,
    status                  VARCHAR(40)  NOT NULL DEFAULT 'PENDENTE',
    publicado_em            TIMESTAMPTZ,
    disponibilizado_em      TIMESTAMPTZ,
    gera_contagem_prazo     BOOLEAN      NOT NULL DEFAULT FALSE,
    criado_em               TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_publicacao_processo  ON pjb_publicacao (processo_id);
CREATE INDEX IF NOT EXISTS idx_publicacao_status    ON pjb_publicacao (status);

CREATE TABLE IF NOT EXISTS pjb_ciencia_publicacao (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processo_id             UUID         NOT NULL,
    publicacao_id           UUID         NOT NULL REFERENCES pjb_publicacao (id),
    parte_id                VARCHAR(120),
    advogado_id             VARCHAR(120),
    data_disponibilizacao   DATE         NOT NULL,
    inicio_prazo            DATE,
    registrada_em           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ciencia_processo   ON pjb_ciencia_publicacao (processo_id);
CREATE INDEX IF NOT EXISTS idx_ciencia_publicacao ON pjb_ciencia_publicacao (publicacao_id);
