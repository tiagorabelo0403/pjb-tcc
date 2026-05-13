CREATE TABLE IF NOT EXISTS tb_pwa_offline_bundle (
    id BIGSERIAL PRIMARY KEY,
    bundle_token VARCHAR(120) NOT NULL UNIQUE,
    processo_id BIGINT NOT NULL,
    solicitante_id BIGINT,
    device_fingerprint VARCHAR(180),
    escopo VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL,
    manifest_hash VARCHAR(128) NOT NULL,
    manifest_json TEXT NOT NULL,
    replay_acoes_json TEXT,
    conflito_resumo TEXT,
    aberto_em TIMESTAMPTZ,
    sincronizado_em TIMESTAMPTZ,
    expira_em TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pwa_offline_bundle_processo FOREIGN KEY (processo_id) REFERENCES tb_processo (id),
    CONSTRAINT fk_pwa_offline_bundle_solicitante FOREIGN KEY (solicitante_id) REFERENCES tb_usuario (id)
);

CREATE INDEX IF NOT EXISTS idx_pwa_offline_bundle_user_status ON tb_pwa_offline_bundle (solicitante_id, status);
CREATE INDEX IF NOT EXISTS idx_pwa_offline_bundle_processo ON tb_pwa_offline_bundle (processo_id);

CREATE TABLE IF NOT EXISTS tb_audiencia_webrtc_sessao (
    id BIGSERIAL PRIMARY KEY,
    audiencia_id BIGINT NOT NULL,
    processo_id BIGINT,
    sessao_token VARCHAR(120) NOT NULL UNIQUE,
    participante_identificador VARCHAR(180) NOT NULL,
    participante_usuario_id BIGINT,
    status VARCHAR(40) NOT NULL,
    ice_servers_csv TEXT,
    offer_hash VARCHAR(128),
    answer_hash VARCHAR(128),
    exigir_biometria BOOLEAN NOT NULL DEFAULT FALSE,
    biometria_status VARCHAR(40),
    transcricao_integral TEXT,
    gravacao_hash VARCHAR(128),
    metricas_json TEXT,
    aberta_em TIMESTAMPTZ,
    encerrada_em TIMESTAMPTZ,
    expira_em TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audiencia_webrtc_sessao_processo FOREIGN KEY (processo_id) REFERENCES tb_processo (id)
);

CREATE INDEX IF NOT EXISTS idx_audiencia_webrtc_participante ON tb_audiencia_webrtc_sessao (participante_usuario_id, status);
CREATE INDEX IF NOT EXISTS idx_audiencia_webrtc_processo ON tb_audiencia_webrtc_sessao (processo_id);

CREATE TABLE IF NOT EXISTS tb_judicial_voice_session (
    id BIGSERIAL PRIMARY KEY,
    processo_id BIGINT NOT NULL,
    magistrado_id BIGINT NOT NULL,
    modo_documento VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    transcricao_integral TEXT,
    relatorio_draft TEXT,
    fundamentacao_draft TEXT,
    dispositivo_draft TEXT,
    comando_resumo TEXT,
    audio_preview_text TEXT,
    aberta_em TIMESTAMPTZ,
    finalizada_em TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_judicial_voice_session_processo FOREIGN KEY (processo_id) REFERENCES tb_processo (id),
    CONSTRAINT fk_judicial_voice_session_magistrado FOREIGN KEY (magistrado_id) REFERENCES tb_usuario (id)
);

CREATE INDEX IF NOT EXISTS idx_judicial_voice_session_magistrado ON tb_judicial_voice_session (magistrado_id, status);
CREATE INDEX IF NOT EXISTS idx_judicial_voice_session_processo ON tb_judicial_voice_session (processo_id);

CREATE TABLE IF NOT EXISTS tb_sigilo_processo_proof_challenge (
    id BIGSERIAL PRIMARY KEY,
    challenge_id VARCHAR(120) NOT NULL UNIQUE,
    processo_id BIGINT NOT NULL,
    solicitante_id BIGINT,
    escopo VARCHAR(80) NOT NULL,
    statement TEXT NOT NULL,
    challenge_payload TEXT NOT NULL,
    commitment_hash VARCHAR(128) NOT NULL,
    response_hash VARCHAR(128),
    status VARCHAR(40) NOT NULL,
    expira_em TIMESTAMPTZ,
    verificado_em TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sigilo_proof_challenge_processo FOREIGN KEY (processo_id) REFERENCES tb_processo (id),
    CONSTRAINT fk_sigilo_proof_challenge_solicitante FOREIGN KEY (solicitante_id) REFERENCES tb_usuario (id)
);

CREATE INDEX IF NOT EXISTS idx_sigilo_proof_challenge_processo ON tb_sigilo_processo_proof_challenge (processo_id, status);
CREATE INDEX IF NOT EXISTS idx_sigilo_proof_challenge_user ON tb_sigilo_processo_proof_challenge (solicitante_id, status);

ALTER TABLE tb_plenario_virtual_voto
    ADD COLUMN IF NOT EXISTS homomorphic_commitment VARCHAR(128),
    ADD COLUMN IF NOT EXISTS homomorphic_tally_blob TEXT,
    ADD COLUMN IF NOT EXISTS zk_proof_hash VARCHAR(128);

CREATE INDEX IF NOT EXISTS idx_plenario_virtual_voto_homomorphic ON tb_plenario_virtual_voto (sessao_id, homomorphic_commitment);
