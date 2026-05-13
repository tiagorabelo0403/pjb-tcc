-- Serviços Judiciais (PJB 2026) - Certidões

CREATE TABLE IF NOT EXISTS tb_certidao_template (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    titulo VARCHAR(260) NOT NULL,
    descricao TEXT,
    corpo_template TEXT NOT NULL,
    ativo BOOLEAN DEFAULT TRUE,
    criado_em TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_certidao_emitida (
    id UUID PRIMARY KEY,
    processo_id BIGINT,
    template_code VARCHAR(80) NOT NULL,
    solicitante_id BIGINT,
    status VARCHAR(40) NOT NULL,
    documento_id UUID,
    protocolo VARCHAR(80),
    parametros_json TEXT,
    criado_em TIMESTAMP,
    atualizado_em TIMESTAMP
);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_cert_emitida_processo') THEN
        ALTER TABLE tb_certidao_emitida
            ADD CONSTRAINT fk_cert_emitida_processo
            FOREIGN KEY (processo_id) REFERENCES tb_processo(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_cert_emitida_documento') THEN
        ALTER TABLE tb_certidao_emitida
            ADD CONSTRAINT fk_cert_emitida_documento
            FOREIGN KEY (documento_id) REFERENCES tb_documento_processual(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_cert_emitida_processo ON tb_certidao_emitida(processo_id);
CREATE INDEX IF NOT EXISTS idx_cert_emitida_template ON tb_certidao_emitida(template_code);
