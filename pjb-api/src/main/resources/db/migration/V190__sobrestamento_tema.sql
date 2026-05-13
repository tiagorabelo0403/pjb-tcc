CREATE TABLE pjb_sobrestamento_tema (
 id BIGSERIAL PRIMARY KEY,
 processo_id BIGINT NOT NULL REFERENCES tb_processo(id) ON DELETE CASCADE,
 tema_id BIGINT NOT NULL REFERENCES tb_tema_repercussao_geral(id),
 status_anterior VARCHAR(64) NOT NULL,
 sobrestado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 retomado_em TIMESTAMPTZ,
 resultado_aplicado VARCHAR(64),
 operador_id BIGINT REFERENCES tb_usuario(id),
 CONSTRAINT uk_sobrestamento_processo_tema UNIQUE (processo_id, tema_id)
);
CREATE INDEX idx_sobrestamento_tema ON pjb_sobrestamento_tema (tema_id);
CREATE INDEX idx_sobrestamento_processo ON pjb_sobrestamento_tema (processo_id);
