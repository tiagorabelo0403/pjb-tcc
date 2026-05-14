-- V248: perito e laudo pericial — designação e controle

CREATE TABLE IF NOT EXISTS pjb_perito (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome                VARCHAR(255) NOT NULL,
    cpf                 CHAR(11)     NOT NULL UNIQUE,
    especialidades      TEXT[]       NOT NULL DEFAULT '{}',
    comarca_id          VARCHAR(60)  NOT NULL,
    comarcas_atendidas  TEXT[]       NOT NULL DEFAULT '{}',
    credenciado_cnj     BOOLEAN      NOT NULL DEFAULT FALSE,
    ativo               BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_perito_comarca ON pjb_perito (comarca_id);

CREATE TABLE IF NOT EXISTS pjb_pericia (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processo_id         UUID         NOT NULL,
    perito_id           UUID         NOT NULL REFERENCES pjb_perito (id),
    especialidade       VARCHAR(120) NOT NULL,
    data_designacao     DATE         NOT NULL,
    data_entrega_prevista DATE,
    laudo_entregue      BOOLEAN      NOT NULL DEFAULT FALSE,
    laudo_assinado      BOOLEAN      NOT NULL DEFAULT FALSE,
    honorarios_valor    NUMERIC(18,2),
    honorarios_depositados BOOLEAN  NOT NULL DEFAULT FALSE,
    quesitos_respondidos BOOLEAN    NOT NULL DEFAULT FALSE,
    partes_intimadas    BOOLEAN      NOT NULL DEFAULT FALSE,
    criado_em           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_pericia_processo ON pjb_pericia (processo_id);
CREATE INDEX IF NOT EXISTS idx_pericia_perito   ON pjb_pericia (perito_id);
