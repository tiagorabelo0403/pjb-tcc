-- V250: exportação de movimentações para o DataJud/CNJ (Round 28AI)

CREATE TABLE IF NOT EXISTS pjb_datajud_movimentacao (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processo_id             UUID         NOT NULL,
    numero_cnj              VARCHAR(25)  NOT NULL,
    codigo_movimentacao     VARCHAR(10)  NOT NULL,
    descricao_movimentacao  VARCHAR(255),
    data_movimentacao       DATE         NOT NULL,
    rito                    VARCHAR(80),
    classe_judicial         VARCHAR(120),
    assunto_principal       VARCHAR(120),
    orgao_julgador          VARCHAR(120),
    tribunal                VARCHAR(60)  NOT NULL,
    exportado               BOOLEAN      NOT NULL DEFAULT FALSE,
    exportado_em            TIMESTAMPTZ,
    criado_em               TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_datajud_processo   ON pjb_datajud_movimentacao (processo_id);
CREATE INDEX IF NOT EXISTS idx_datajud_exportado  ON pjb_datajud_movimentacao (exportado, tribunal)
    WHERE exportado = FALSE;
CREATE INDEX IF NOT EXISTS idx_datajud_tribunal   ON pjb_datajud_movimentacao (tribunal, data_movimentacao DESC);
