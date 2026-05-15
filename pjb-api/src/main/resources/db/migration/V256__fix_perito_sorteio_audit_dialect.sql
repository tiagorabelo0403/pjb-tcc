DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_name = 'tb_perito_sorteio_audit'
    ) THEN
        CREATE TABLE tb_perito_sorteio_audit (
            id                   BIGSERIAL    PRIMARY KEY,
            actor_id             BIGINT       NOT NULL,
            processo_id          BIGINT,
            disponibilidade_id   BIGINT       NOT NULL,
            perito_id            BIGINT       NOT NULL,
            especialidade_codigo VARCHAR(120) NOT NULL,
            comarca              VARCHAR(120),
            data_referencia      VARCHAR(20)  NOT NULL,
            score                DOUBLE PRECISION NOT NULL,
            nomeacoes_ativas     BIGINT       NOT NULL,
            candidatos_eligiveis INTEGER      NOT NULL,
            fundamentos_json     TEXT,
            hash_integridade     VARCHAR(64)  NOT NULL,
            created_at           TIMESTAMP,
            CONSTRAINT fk_perito_sorteio_actor         FOREIGN KEY (actor_id)           REFERENCES tb_usuario(id),
            CONSTRAINT fk_perito_sorteio_processo      FOREIGN KEY (processo_id)         REFERENCES tb_processo(id),
            CONSTRAINT fk_perito_sorteio_disponibilidade FOREIGN KEY (disponibilidade_id) REFERENCES tb_perito_disponibilidade(id),
            CONSTRAINT fk_perito_sorteio_perito        FOREIGN KEY (perito_id)           REFERENCES tb_usuario(id)
        );

        CREATE INDEX idx_perito_sorteio_actor   ON tb_perito_sorteio_audit (actor_id,   created_at);
        CREATE INDEX idx_perito_sorteio_processo ON tb_perito_sorteio_audit (processo_id, created_at);
        CREATE INDEX idx_perito_sorteio_perito  ON tb_perito_sorteio_audit (perito_id,  created_at);
    END IF;
END;
$$;
