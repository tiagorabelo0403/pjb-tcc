ALTER TABLE tb_marketplace_webhook_endpoint
    ADD COLUMN IF NOT EXISTS signing_secret_ciphertext TEXT NULL;

ALTER TABLE tb_marketplace_webhook_delivery
    ADD COLUMN IF NOT EXISTS payload_json TEXT NULL,
    ADD COLUMN IF NOT EXISTS last_dispatch_at TIMESTAMP NULL;

CREATE TABLE IF NOT EXISTS tb_perito_sorteio_audit (
    id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT NOT NULL,
    processo_id BIGINT NULL,
    disponibilidade_id BIGINT NOT NULL,
    perito_id BIGINT NOT NULL,
    especialidade_codigo VARCHAR(120) NOT NULL,
    comarca VARCHAR(120) NULL,
    data_referencia VARCHAR(20) NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    nomeacoes_ativas BIGINT NOT NULL,
    candidatos_eligiveis INT NOT NULL,
    fundamentos_json TEXT NULL,
    hash_integridade VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NULL,
    CONSTRAINT fk_perito_sorteio_actor FOREIGN KEY (actor_id) REFERENCES tb_usuario(id),
    CONSTRAINT fk_perito_sorteio_processo FOREIGN KEY (processo_id) REFERENCES tb_processo(id),
    CONSTRAINT fk_perito_sorteio_disponibilidade FOREIGN KEY (disponibilidade_id) REFERENCES tb_perito_disponibilidade(id),
    CONSTRAINT fk_perito_sorteio_perito FOREIGN KEY (perito_id) REFERENCES tb_usuario(id)
);

CREATE INDEX IF NOT EXISTS idx_perito_sorteio_actor ON tb_perito_sorteio_audit(actor_id, created_at);
CREATE INDEX IF NOT EXISTS idx_perito_sorteio_processo ON tb_perito_sorteio_audit(processo_id, created_at);
CREATE INDEX IF NOT EXISTS idx_perito_sorteio_perito ON tb_perito_sorteio_audit(perito_id, created_at);
