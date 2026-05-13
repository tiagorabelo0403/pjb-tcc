-- Serviços Judiciais (PJB 2026) - Audiências

CREATE TABLE IF NOT EXISTS tb_audiencia (
    id BIGSERIAL PRIMARY KEY,
    processo_id BIGINT,
    tipo VARCHAR(60) NOT NULL,
    modalidade VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    data_hora TIMESTAMP NOT NULL,
    duracao_min INT,
    local VARCHAR(260),
    link_video VARCHAR(600),
    pauta TEXT,
    notas TEXT,
    criado_por BIGINT,
    criado_em TIMESTAMP,
    atualizado_em TIMESTAMP
);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_audiencia_processo') THEN
        ALTER TABLE tb_audiencia
            ADD CONSTRAINT fk_audiencia_processo
            FOREIGN KEY (processo_id) REFERENCES tb_processo(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_audiencia_processo ON tb_audiencia(processo_id);
CREATE INDEX IF NOT EXISTS idx_audiencia_data ON tb_audiencia(data_hora);
