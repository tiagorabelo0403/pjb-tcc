CREATE TABLE tb_requisito_documental (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    uuid                        UUID NOT NULL,
    rito_codigo                 VARCHAR(80) NOT NULL,
    tipo_documento              VARCHAR(120) NOT NULL,
    obrigatorio                 BOOLEAN NOT NULL DEFAULT TRUE,
    severidade                  VARCHAR(20) NOT NULL,
    fonte_normativa_tipo        VARCHAR(40) NOT NULL,
    fonte_normativa_identificador VARCHAR(200),
    fundamento_resumido         VARCHAR(500),
    grau_exigibilidade          VARCHAR(40) NOT NULL,
    autoridade_origem           VARCHAR(200),
    aplica_a_tipo_parte         VARCHAR(60),
    aplica_a_representante      VARCHAR(60),
    tipo_condicao               VARCHAR(60) NOT NULL DEFAULT 'SEMPRE',
    condicao_parametro          VARCHAR(120),
    nivel_sensibilidade         VARCHAR(20) NOT NULL DEFAULT 'PUBLICO',
    vigente_a_partir_de         DATE NOT NULL,
    vigente_ate                 DATE,
    versao                      VARCHAR(40) NOT NULL,
    imutavel                    BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em                   TIMESTAMPTZ NOT NULL,
    criado_por                  BIGINT,
    CONSTRAINT uq_requisito_documental_uuid UNIQUE (uuid),
    CONSTRAINT uq_requisito_documental_chave UNIQUE (
        rito_codigo, tipo_documento, vigente_a_partir_de, tipo_condicao, aplica_a_tipo_parte
    ),
    CONSTRAINT ck_requisito_severidade CHECK (severidade IN ('BLOQUEANTE', 'ADVERTENCIA')),
    CONSTRAINT ck_requisito_grau CHECK (
        grau_exigibilidade IN ('ABSOLUTO', 'RELATIVO', 'DISPENSAVEL_COM_JUSTIFICATIVA')
    ),
    CONSTRAINT ck_requisito_condicao CHECK (
        tipo_condicao IN ('SEMPRE', 'PEDE_JUSTICA_GRATUITA', 'ENVOLVE_MENOR', 'ENVOLVE_INCAPAZ',
                          'ADVOGADO_CONSTITUIDO', 'ACAO_CONTRATUAL', 'EXIGE_QUALIFICACAO_COMPLETA',
                          'SEGREDO_JUSTICA')
    )
);

CREATE INDEX idx_requisito_rito ON tb_requisito_documental (rito_codigo);
CREATE INDEX idx_requisito_vigencia ON tb_requisito_documental (rito_codigo, vigente_a_partir_de);

INSERT INTO tb_requisito_documental (
    uuid, rito_codigo, tipo_documento, obrigatorio, severidade,
    fonte_normativa_tipo, fonte_normativa_identificador, fundamento_resumido,
    grau_exigibilidade, autoridade_origem,
    aplica_a_tipo_parte, aplica_a_representante,
    tipo_condicao, nivel_sensibilidade,
    vigente_a_partir_de, vigente_ate, versao, imutavel, criado_em
) VALUES

-- COMUM_ORDINARIO: qualificação das partes (CPC art. 319, II)
(gen_random_uuid(), 'COMUM_ORDINARIO', 'DOCUMENTO_IDENTIDADE', TRUE, 'BLOQUEANTE',
 'LEI', 'CPC art. 319, II',
 'A petição inicial indicará a qualificação das partes. Documento de identidade exigido para qualificação.',
 'ABSOLUTO', 'Código de Processo Civil (Lei nº 13.105/2015)',
 NULL, NULL,
 'SEMPRE', 'PUBLICO',
 '2015-03-16', NULL, 'v1.0', TRUE, NOW()),

-- COMUM_ORDINARIO: procuração — apenas se ADVOGADO_CONSTITUIDO (CPC art. 287)
(gen_random_uuid(), 'COMUM_ORDINARIO', 'PROCURACAO', TRUE, 'BLOQUEANTE',
 'LEI', 'CPC art. 287',
 'Havendo advogado constituído, a procuração deve ser juntada na primeira oportunidade processual.',
 'DISPENSAVEL_COM_JUSTIFICATIVA', 'Código de Processo Civil (Lei nº 13.105/2015)',
 NULL, 'ADVOGADO_PRIVADO',
 'ADVOGADO_CONSTITUIDO', 'PUBLICO',
 '2015-03-16', NULL, 'v1.0', TRUE, NOW()),

-- COMUM_ORDINARIO: declaração de hipossuficiência — apenas se PEDE_JUSTICA_GRATUITA (CPC art. 99 + Lei 1.060/1950)
(gen_random_uuid(), 'COMUM_ORDINARIO', 'DECLARACAO_HIPOSSUFICIENCIA', TRUE, 'BLOQUEANTE',
 'LEI', 'Lei nº 1.060/1950 c/c CPC art. 99',
 'O pedido de gratuidade de justiça requer declaração de hipossuficiência ou documentos que a comprovem.',
 'RELATIVO', 'Código de Processo Civil (Lei nº 13.105/2015) e Lei de Gratuidade de Justiça',
 NULL, NULL,
 'PEDE_JUSTICA_GRATUITA', 'PUBLICO',
 '2015-03-16', NULL, 'v1.0', TRUE, NOW()),

-- TRABALHISTA_ORDINARIO: CTPS — vínculo empregatício (CLT art. 41 + TST)
(gen_random_uuid(), 'TRABALHISTA_ORDINARIO', 'CTPS', TRUE, 'BLOQUEANTE',
 'LEI', 'CLT art. 41 c/c TST IN 39/2016',
 'A CTPS é documento essencial para comprovar vínculo empregatício na ação trabalhista ordinária.',
 'RELATIVO', 'Consolidação das Leis do Trabalho e Instrução Normativa TST nº 39/2016',
 NULL, NULL,
 'SEMPRE', 'PUBLICO',
 '2016-03-16', NULL, 'v1.0', TRUE, NOW()),

-- TRABALHISTA_ORDINARIO: qualificação (SEMPRE)
(gen_random_uuid(), 'TRABALHISTA_ORDINARIO', 'DOCUMENTO_IDENTIDADE', TRUE, 'BLOQUEANTE',
 'LEI', 'CLT art. 840 §1º',
 'A reclamação trabalhista exige qualificação das partes, incluindo documento de identidade do reclamante.',
 'ABSOLUTO', 'Consolidação das Leis do Trabalho',
 NULL, NULL,
 'SEMPRE', 'PUBLICO',
 '1943-08-09', NULL, 'v1.0', TRUE, NOW()),

-- TRABALHISTA_ORDINARIO: procuração — apenas se ADVOGADO_CONSTITUIDO
(gen_random_uuid(), 'TRABALHISTA_ORDINARIO', 'PROCURACAO', TRUE, 'BLOQUEANTE',
 'LEI', 'CPC art. 287 c/c CLT art. 791',
 'Se representado por advogado, a procuração ad judicia deve ser juntada à reclamação trabalhista.',
 'DISPENSAVEL_COM_JUSTIFICATIVA', 'Consolidação das Leis do Trabalho e CPC subsidiário',
 NULL, 'ADVOGADO_PRIVADO',
 'ADVOGADO_CONSTITUIDO', 'PUBLICO',
 '1943-08-09', NULL, 'v1.0', TRUE, NOW()),

-- ESPECIAL_HABEAS_CORPUS: qualificação (SEMPRE — não exige OAB, mas exige identificação do paciente)
(gen_random_uuid(), 'ESPECIAL_HABEAS_CORPUS', 'DOCUMENTO_IDENTIDADE', TRUE, 'ADVERTENCIA',
 'LEI', 'CPP art. 654 §1º',
 'A petição de habeas corpus deve conter a qualificação do paciente. Documento de identidade recomendado.',
 'RELATIVO', 'Código de Processo Penal',
 NULL, NULL,
 'SEMPRE', 'PUBLICO',
 '1941-10-03', NULL, 'v1.0', TRUE, NOW()),

-- ESPECIAL_HABEAS_CORPUS: BO como advertência (quando disponível, documenta a prisão ilegal alegada)
(gen_random_uuid(), 'ESPECIAL_HABEAS_CORPUS', 'BOLETIM_OCORRENCIA', FALSE, 'ADVERTENCIA',
 'REGRA_INTERNA', NULL,
 'O boletim de ocorrência ou auto de prisão, quando disponível, documenta a coação ilegal alegada no HC.',
 'DISPENSAVEL_COM_JUSTIFICATIVA', 'Prática processual penal — não obrigatório por lei',
 NULL, NULL,
 'SEMPRE', 'PUBLICO',
 '2024-01-01', NULL, 'v1.0', TRUE, NOW());
