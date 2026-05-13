CREATE TABLE balcao_virtual_atendimento (
    id                  BIGSERIAL PRIMARY KEY,
    protocolo           VARCHAR(25)  NOT NULL UNIQUE,
    tipo                VARCHAR(60)  NOT NULL,
    status              VARCHAR(50)  NOT NULL,
    solicitante_nome    VARCHAR(255) NOT NULL,
    solicitante_oab     VARCHAR(20),
    solicitante_oab_uf  VARCHAR(2),
    solicitante_cpf     VARCHAR(14),
    numero_processo     VARCHAR(25),
    vara                VARCHAR(120),
    descricao           TEXT,
    atendido_por        VARCHAR(255),
    observacoes         TEXT,
    criado_em           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    chamado_em          TIMESTAMP WITH TIME ZONE,
    iniciado_em         TIMESTAMP WITH TIME ZONE,
    encerrado_em        TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_balcao_status_criado  ON balcao_virtual_atendimento(status, criado_em);
CREATE INDEX idx_balcao_vara           ON balcao_virtual_atendimento(vara)   WHERE vara IS NOT NULL;
CREATE INDEX idx_balcao_protocolo      ON balcao_virtual_atendimento(protocolo);

ALTER TABLE balcao_virtual_atendimento ENABLE ROW LEVEL SECURITY;
