CREATE TABLE IF NOT EXISTS tb_expedicao_judicial (
    id BIGSERIAL PRIMARY KEY,
    expedicao_uuid VARCHAR(36) NOT NULL UNIQUE,
    processo_id BIGINT NOT NULL,
    numero_unificado VARCHAR(25),
    tipo_comunicacao VARCHAR(60) NOT NULL,
    modalidade VARCHAR(60) NOT NULL,
    status VARCHAR(40) NOT NULL,
    tipo_destinatario VARCHAR(40) NOT NULL,
    destinatario_nome VARCHAR(200) NOT NULL,
    destinatario_documento VARCHAR(40) NOT NULL,
    destinatario_email VARCHAR(200),
    destinatario_telefone VARCHAR(20),
    destinatario_endereco_entrega VARCHAR(500),
    canal_digital_utilizado VARCHAR(120),
    expedida_em TIMESTAMP NOT NULL,
    presuncao_entrega_em TIMESTAMP,
    confirmacao_entrega_em TIMESTAMP,
    leitura_confirmada_em TIMESTAMP,
    prazo_resposta_inicio_em TIMESTAMP,
    tentativas_realizadas INTEGER NOT NULL DEFAULT 1,
    proxima_tentativa_em TIMESTAMP,
    frustrada_em TIMESTAMP,
    motivo_frustracao VARCHAR(1000),
    fallback_modalidade VARCHAR(60),
    ramo_direito VARCHAR(50),
    grau_jurisdicao VARCHAR(30),
    instancia_expedidora VARCHAR(120),
    juiz_responsavel_id BIGINT,
    servidor_expedidor_id BIGINT,
    mandado_id BIGINT,
    oficial_id BIGINT,
    codigo_rastreio_correio VARCHAR(20),
    numero_edital VARCHAR(80),
    ip_leitura VARCHAR(45),
    device_fingerprint VARCHAR(256),
    govbr_session_token VARCHAR(256),
    acuse_hash VARCHAR(64),
    hash_integridade VARCHAR(64) NOT NULL UNIQUE,
    hash_anterior VARCHAR(64),
    fundamentacao_legal VARCHAR(1200),
    evasao_detectada BOOLEAN NOT NULL DEFAULT FALSE,
    evasao_evidencias VARCHAR(1000),
    escalonado_para_juiz BOOLEAN NOT NULL DEFAULT FALSE,
    escalonamento_em TIMESTAMP,
    ciencia_judicial_em TIMESTAMP,
    version BIGINT,
    CONSTRAINT fk_expedicao_processo FOREIGN KEY (processo_id) REFERENCES tb_processo (id),
    CONSTRAINT fk_expedicao_juiz FOREIGN KEY (juiz_responsavel_id) REFERENCES tb_usuario (id),
    CONSTRAINT fk_expedicao_servidor FOREIGN KEY (servidor_expedidor_id) REFERENCES tb_usuario (id),
    CONSTRAINT fk_expedicao_oficial FOREIGN KEY (oficial_id) REFERENCES tb_usuario (id),
    CONSTRAINT chk_expedicao_tentativas_nao_negativas CHECK (tentativas_realizadas >= 0)
);

CREATE INDEX IF NOT EXISTS idx_expedicao_processo_id ON tb_expedicao_judicial (processo_id);
CREATE INDEX IF NOT EXISTS idx_expedicao_destinatario_doc ON tb_expedicao_judicial (destinatario_documento);
CREATE INDEX IF NOT EXISTS idx_expedicao_status ON tb_expedicao_judicial (status);
CREATE INDEX IF NOT EXISTS idx_expedicao_modalidade ON tb_expedicao_judicial (modalidade);
CREATE INDEX IF NOT EXISTS idx_expedicao_expedida_em ON tb_expedicao_judicial (expedida_em);
CREATE UNIQUE INDEX IF NOT EXISTS idx_expedicao_hash_integridade ON tb_expedicao_judicial (hash_integridade);
CREATE INDEX IF NOT EXISTS idx_expedicao_frustracao_recente ON tb_expedicao_judicial (destinatario_documento, frustrada_em);
CREATE INDEX IF NOT EXISTS idx_expedicao_evasao_escalonamento ON tb_expedicao_judicial (evasao_detectada, escalonado_para_juiz, status);
