CREATE TABLE tb_requisito_documental_equivalencia (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    uuid                        UUID NOT NULL,
    requisito_id                BIGINT NOT NULL REFERENCES tb_requisito_documental(id),
    tipo_documento_aceito       VARCHAR(120) NOT NULL,
    justificativa               VARCHAR(300),
    severidade_se_substituto    VARCHAR(20) NOT NULL DEFAULT 'ADVERTENCIA',
    criado_em                   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_equivalencia_uuid UNIQUE (uuid),
    CONSTRAINT uq_equivalencia_requisito_tipo UNIQUE (requisito_id, tipo_documento_aceito),
    CONSTRAINT ck_equivalencia_severidade CHECK (severidade_se_substituto IN ('BLOQUEANTE', 'ADVERTENCIA'))
);

INSERT INTO tb_requisito_documental_equivalencia (
    uuid, requisito_id, tipo_documento_aceito, justificativa, severidade_se_substituto, criado_em
)
SELECT
    gen_random_uuid(),
    r.id,
    'CONTRATO_TRABALHO',
    'Contrato de trabalho escrito aceito como substituto da CTPS para comprovação de vínculo empregatício.',
    'ADVERTENCIA',
    NOW()
FROM tb_requisito_documental r
WHERE r.rito_codigo = 'TRABALHISTA_ORDINARIO'
  AND r.tipo_documento = 'CTPS'
  AND r.versao = 'v1.0';
