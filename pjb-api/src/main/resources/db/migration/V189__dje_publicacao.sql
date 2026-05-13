CREATE TABLE pjb_dje_publicacao (
 id BIGSERIAL PRIMARY KEY,
 processo_id BIGINT NOT NULL REFERENCES tb_processo(id) ON DELETE RESTRICT,
 tipo_ato VARCHAR(64) NOT NULL,
 conteudo_hash VARCHAR(128) NOT NULL,
 edicao_dje VARCHAR(32),
 data_disponibilizacao DATE,
 data_publicacao DATE,
 prazo_comeca_em DATE,
 tribunal_codigo VARCHAR(32),
 status VARCHAR(32) NOT NULL DEFAULT 'PENDENTE_ENVIO',
 enviado_em TIMESTAMPTZ,
 publicado_em TIMESTAMPTZ,
 failure_reason TEXT,
 partes_notificadas BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_dje_processo ON pjb_dje_publicacao (processo_id);
CREATE INDEX idx_dje_status ON pjb_dje_publicacao (status, data_disponibilizacao);
