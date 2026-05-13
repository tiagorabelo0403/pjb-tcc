ALTER TABLE tb_perito_nomeacao
    ADD COLUMN IF NOT EXISTS respondido_em TIMESTAMP NULL,
    ADD COLUMN IF NOT EXISTS respondido_por BIGINT NULL;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_perito_nomeacao_respondido_por') THEN
        ALTER TABLE tb_perito_nomeacao
            ADD CONSTRAINT fk_perito_nomeacao_respondido_por
            FOREIGN KEY (respondido_por) REFERENCES tb_usuario(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_perito_nomeacao_status ON tb_perito_nomeacao(status);
CREATE INDEX IF NOT EXISTS idx_perito_nomeacao_respondido_por ON tb_perito_nomeacao(respondido_por);

CREATE TABLE IF NOT EXISTS tb_perfil_behavior_baseline (
    id BIGSERIAL PRIMARY KEY,
    tipo_usuario VARCHAR(80) NOT NULL,
    expected_volume INTEGER NOT NULL,
    alert_threshold_ratio NUMERIC(10,4) NOT NULL,
    anomaly_threshold_ratio NUMERIC(10,4) NOT NULL,
    rationale VARCHAR(240),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_perfil_behavior_baseline_tipo UNIQUE (tipo_usuario)
);

CREATE INDEX IF NOT EXISTS idx_perfil_behavior_baseline_ativo ON tb_perfil_behavior_baseline(ativo);

INSERT INTO tb_perfil_behavior_baseline (tipo_usuario, expected_volume, alert_threshold_ratio, anomaly_threshold_ratio, rationale, ativo)
VALUES
    ('PERITO', 25, 1.1000, 1.5000, 'Perito tende a operar em poucos processos simultâneos.', TRUE),
    ('PERITO_MEDICO', 25, 1.1000, 1.5000, 'Perícia médica exige carteira limitada e agenda técnica.', TRUE),
    ('PSICOLOGO_JUDICIAL', 18, 1.1000, 1.5000, 'Perfil psicossocial acessa volume reduzido e sensível.', TRUE),
    ('ASSISTENTE_SOCIAL_JUDICIAL', 18, 1.1000, 1.5000, 'Perfil psicossocial acessa volume reduzido e sensível.', TRUE),
    ('OFICIAL_JUSTICA', 80, 1.1200, 1.5000, 'Oficial de justiça opera carteira territorializada.', TRUE),
    ('OFICIAL_JUSTICA_AVALIADOR', 90, 1.1200, 1.5500, 'Oficial avaliador possui carteira ampliada por diligências de avaliação.', TRUE),
    ('DELEGADO_POLICIA', 120, 1.1500, 1.6000, 'Delegado mantém painel amplo por circunscrição.', TRUE),
    ('DELEGADO_POLICIA_FEDERAL', 140, 1.1500, 1.6000, 'Delegado federal opera carteira ampliada e interestadual.', TRUE),
    ('ASSESSOR_JUDICIAL', 160, 1.1200, 1.5000, 'Assessor acompanha múltiplos gabinetes e minutas.', TRUE),
    ('MEMBRO_MINISTERIO_PUBLICO', 120, 1.1200, 1.5000, 'MP acompanha volume elevado de manifestações e diligências.', TRUE),
    ('LEILOEIRO_JUDICIAL', 40, 1.1000, 1.4500, 'Leiloeiro atua em carteira moderada e especializada.', TRUE),
    ('TABELIAO', 110, 1.1000, 1.4500, 'Serventia extrajudicial acessa atos vinculados a penhoras e averbações.', TRUE),
    ('MINISTRO', 400, 1.1000, 1.4500, 'Gabinete superior opera acervo amplo e colegiado.', TRUE)
ON CONFLICT (tipo_usuario) DO UPDATE SET
    expected_volume = EXCLUDED.expected_volume,
    alert_threshold_ratio = EXCLUDED.alert_threshold_ratio,
    anomaly_threshold_ratio = EXCLUDED.anomaly_threshold_ratio,
    rationale = EXCLUDED.rationale,
    ativo = EXCLUDED.ativo,
    updated_at = NOW();
