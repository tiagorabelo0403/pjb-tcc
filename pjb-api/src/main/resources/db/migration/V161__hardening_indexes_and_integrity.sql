CREATE INDEX IF NOT EXISTS idx_marketplace_access_token_status_expires
    ON tb_marketplace_access_token (status, expires_at);

CREATE INDEX IF NOT EXISTS idx_marketplace_webhook_delivery_retry
    ON tb_marketplace_webhook_delivery (status, next_retry_at, created_at);

CREATE INDEX IF NOT EXISTS idx_marketplace_webhook_delivery_endpoint_status
    ON tb_marketplace_webhook_delivery (endpoint_id, status, attempts);

CREATE INDEX IF NOT EXISTS idx_pwa_offline_bundle_status_expira
    ON tb_pwa_offline_bundle (status, expira_em);

CREATE INDEX IF NOT EXISTS idx_audiencia_webrtc_status_expira
    ON tb_audiencia_webrtc_sessao (status, expira_em);

CREATE INDEX IF NOT EXISTS idx_judicial_voice_session_status_updated
    ON tb_judicial_voice_session (magistrado_id, status, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_sigilo_proof_challenge_status_expira
    ON tb_sigilo_processo_proof_challenge (status, expira_em);

CREATE INDEX IF NOT EXISTS idx_tema_recurso_repetitivo_status_updated
    ON tb_tema_recurso_repetitivo (status, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_notification_history_tracking_usuario
    ON notification_history (tracking_token, usuario_id);
