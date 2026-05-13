CREATE TABLE pjb_mni_remessa (
 id BIGSERIAL PRIMARY KEY,
 processo_id BIGINT NOT NULL REFERENCES tb_processo(id) ON DELETE RESTRICT,
 tribunal_destino VARCHAR(32) NOT NULL,
 motivo VARCHAR(64) NOT NULL,
 status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
 protocolo_destino VARCHAR(128),
 mni_payload_hash VARCHAR(128),
 tentativas INT NOT NULL DEFAULT 0,
 max_tentativas INT NOT NULL DEFAULT 5,
 proximo_retry_em TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 sent_at TIMESTAMPTZ,
 confirmed_at TIMESTAMPTZ,
 failure_reason TEXT,
 CONSTRAINT uk_mni_remessa_processo_destino UNIQUE (processo_id, tribunal_destino, motivo)
);
CREATE INDEX idx_mni_remessa_status ON pjb_mni_remessa (status, proximo_retry_em);
CREATE TABLE pjb_mni_recepcao (
 id BIGSERIAL PRIMARY KEY,
 tribunal_origem VARCHAR(32) NOT NULL,
 numero_unificado VARCHAR(64) NOT NULL,
 processo_id_local BIGINT REFERENCES tb_processo(id) ON DELETE SET NULL,
 motivo VARCHAR(64),
 mni_payload_hash VARCHAR(128) NOT NULL UNIQUE,
 received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 processed_at TIMESTAMPTZ,
 status VARCHAR(32) NOT NULL DEFAULT 'RECEIVED'
);
