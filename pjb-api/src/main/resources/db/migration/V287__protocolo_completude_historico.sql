CREATE TABLE tb_protocolo_validacao_historico (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    uuid                    UUID NOT NULL,
    protocolo_id            BIGINT NOT NULL,
    status_resultante       VARCHAR(40) NOT NULL,
    versao_regra_aplicada   VARCHAR(40) NOT NULL,
    violacoes_json          JSONB NOT NULL,
    documentos_hash         VARCHAR(64) NOT NULL,
    origem_validacao        VARCHAR(20) NOT NULL,
    executado_por           BIGINT,
    executado_em            TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_validacao_historico_uuid UNIQUE (uuid),
    CONSTRAINT ck_historico_status CHECK (
        status_resultante IN ('RECEBIDO', 'EM_VALIDACAO', 'PENDENTE_DOCUMENTACAO',
                              'COMPLETO', 'DISPENSADO', 'DISTRIBUIDO', 'CANCELADO')
    ),
    CONSTRAINT ck_historico_origem CHECK (
        origem_validacao IN ('PROTOCOLO', 'REENVIO', 'OVERRIDE', 'SCHEDULER', 'PRE_VALIDACAO')
    )
);

CREATE INDEX idx_validacao_historico_protocolo
    ON tb_protocolo_validacao_historico (protocolo_id, executado_em);
