CREATE TABLE IF NOT EXISTS acordos_homologados (
    id                      BIGSERIAL       PRIMARY KEY,
    proposta_acordo_id      BIGINT          NOT NULL UNIQUE,
    processo_id             BIGINT          NOT NULL UNIQUE,
    url_pdf_homologado      TEXT            NOT NULL,
    juiz_homologador_id     BIGINT          NOT NULL,
    data_homologacao        TIMESTAMP       NOT NULL DEFAULT now(),
    hash_assinatura_juiz    TEXT,
    hash_assinatura_parte1  TEXT,
    hash_assinatura_parte2  TEXT,
    CONSTRAINT fk_ah_proposta   FOREIGN KEY (proposta_acordo_id) REFERENCES propostas_acordo(id),
    CONSTRAINT fk_ah_processo   FOREIGN KEY (processo_id)        REFERENCES tb_processo(id),
    CONSTRAINT fk_ah_juiz       FOREIGN KEY (juiz_homologador_id) REFERENCES tb_usuario(id)
);

CREATE INDEX IF NOT EXISTS idx_acordos_homologados_processo ON acordos_homologados (processo_id);
CREATE INDEX IF NOT EXISTS idx_acordos_homologados_juiz     ON acordos_homologados (juiz_homologador_id);
