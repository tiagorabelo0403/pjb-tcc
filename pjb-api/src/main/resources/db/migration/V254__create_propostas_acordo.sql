CREATE TABLE IF NOT EXISTS propostas_acordo (
    id                        BIGSERIAL    PRIMARY KEY,
    uuid                      UUID         NOT NULL UNIQUE,
    processo_id               BIGINT       NOT NULL,
    proponente_id             BIGINT       NOT NULL,
    equipe_id                 BIGINT       NOT NULL,
    termos_html               TEXT         NOT NULL,
    valor_acordo              NUMERIC(19,2),
    status                    VARCHAR(30)  NOT NULL DEFAULT 'EM_NEGOCIACAO',
    hash_assinatura_parte1    VARCHAR(255),
    data_assinatura_parte1    TIMESTAMP,
    hash_assinatura_parte2    VARCHAR(255),
    data_assinatura_parte2    TIMESTAMP,
    data_criacao              TIMESTAMP    NOT NULL,
    data_atualizacao          TIMESTAMP    NOT NULL,
    ia_run_id                 UUID,
    aprovado_por_advogado_id  BIGINT,
    justificativa_aprovacao   TEXT,
    data_aprovacao            TIMESTAMP,
    ia_profile_id             BIGINT,
    ia_settings               VARCHAR(1000),
    CONSTRAINT fk_proposta_processo   FOREIGN KEY (processo_id)   REFERENCES tb_processo(id),
    CONSTRAINT fk_proposta_proponente FOREIGN KEY (proponente_id) REFERENCES tb_usuario(id),
    CONSTRAINT fk_proposta_equipe     FOREIGN KEY (equipe_id)     REFERENCES equipes(id)
);

CREATE INDEX IF NOT EXISTS idx_proposta_uuid
    ON propostas_acordo (uuid);

CREATE INDEX IF NOT EXISTS idx_proposta_processo_status
    ON propostas_acordo (processo_id, status);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_batna_proposta'
          AND conrelid = 'tb_batna_relatorio'::regclass
    ) THEN
        ALTER TABLE tb_batna_relatorio
            ADD CONSTRAINT fk_batna_proposta
            FOREIGN KEY (proposta_acordo_id) REFERENCES propostas_acordo(id);
    END IF;
END $$;
