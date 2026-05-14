-- V246: citação — modalidade, resultado e controle (Round 28AI)

CREATE TABLE IF NOT EXISTS pjb_citacao (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processo_id         UUID         NOT NULL,
    parte_id            VARCHAR(120) NOT NULL,
    modalidade          VARCHAR(40)  NOT NULL,
    mandado_assinado    BOOLEAN      NOT NULL DEFAULT FALSE,
    status              VARCHAR(40)  NOT NULL DEFAULT 'AGUARDANDO',
    data_resultado      DATE,
    tipo_resultado      VARCHAR(40),
    nome_recebedor      VARCHAR(255),
    motivo_negativa     TEXT,
    criado_em           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_citacao_processo ON pjb_citacao (processo_id);
CREATE INDEX IF NOT EXISTS idx_citacao_status   ON pjb_citacao (status);
CREATE INDEX IF NOT EXISTS idx_citacao_parte    ON pjb_citacao (parte_id);
