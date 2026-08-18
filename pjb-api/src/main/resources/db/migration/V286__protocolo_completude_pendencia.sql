CREATE TABLE tb_protocolo_pendencia (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    uuid                    UUID NOT NULL,
    protocolo_id            BIGINT NOT NULL,
    status                  VARCHAR(40) NOT NULL,
    prazo_regularizacao     DATE,
    violacoes_json          JSONB NOT NULL,
    requisitos_versao       VARCHAR(40) NOT NULL,
    documentos_hash         VARCHAR(64) NOT NULL,
    version                 BIGINT NOT NULL DEFAULT 0,
    dispensado_por          BIGINT,
    dispensa_justificativa  VARCHAR(1000),
    criado_em               TIMESTAMPTZ NOT NULL,
    atualizado_em           TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_protocolo_pendencia_uuid UNIQUE (uuid),
    CONSTRAINT ck_pendencia_status CHECK (
        status IN ('RECEBIDO', 'EM_VALIDACAO', 'PENDENTE_DOCUMENTACAO',
                   'COMPLETO', 'DISPENSADO', 'DISTRIBUIDO', 'CANCELADO')
    )
);

CREATE INDEX idx_pendencia_protocolo_status ON tb_protocolo_pendencia (protocolo_id, status);

CREATE UNIQUE INDEX uq_pendencia_ativa_por_protocolo
    ON tb_protocolo_pendencia (protocolo_id)
    WHERE status IN ('PENDENTE_DOCUMENTACAO', 'EM_VALIDACAO');
