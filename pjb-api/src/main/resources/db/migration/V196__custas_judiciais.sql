CREATE TABLE pjb_custa_judicial (
 id BIGSERIAL PRIMARY KEY,
 processo_id BIGINT NOT NULL REFERENCES tb_processo(id) ON DELETE RESTRICT,
 tipo VARCHAR(64) NOT NULL,
 valor NUMERIC(19,2) NOT NULL,
 codigo_receita VARCHAR(16),
 competencia_uf VARCHAR(2),
 isento BOOLEAN NOT NULL DEFAULT FALSE,
 motivo_isencao VARCHAR(255),
 linha_digitavel VARCHAR(120),
 codigo_barras VARCHAR(60),
 pix_payload TEXT,
 pix_txid VARCHAR(77),
 status VARCHAR(32) NOT NULL DEFAULT 'PENDENTE',
 vencimento DATE NOT NULL,
 pago_em TIMESTAMPTZ,
 valor_pago NUMERIC(19,2),
 nosso_numero VARCHAR(64),
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_custa_processo ON pjb_custa_judicial (processo_id);
CREATE INDEX idx_custa_status ON pjb_custa_judicial (status, vencimento) WHERE status = 'PENDENTE';
