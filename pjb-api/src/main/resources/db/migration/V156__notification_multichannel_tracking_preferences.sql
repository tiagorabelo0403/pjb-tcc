CREATE TABLE IF NOT EXISTS tb_usuario_notification_preference (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL UNIQUE,
    allow_email BOOLEAN NOT NULL DEFAULT TRUE,
    allow_push BOOLEAN NOT NULL DEFAULT TRUE,
    allow_whatsapp BOOLEAN NOT NULL DEFAULT FALSE,
    allow_ar_digital BOOLEAN NOT NULL DEFAULT FALSE,
    allow_webhook BOOLEAN NOT NULL DEFAULT FALSE,
    allow_digest BOOLEAN NOT NULL DEFAULT TRUE,
    only_high_priority BOOLEAN NOT NULL DEFAULT FALSE,
    anti_spam_window_minutes INTEGER NOT NULL DEFAULT 30,
    push_endpoint VARCHAR(300),
    whatsapp_number VARCHAR(40),
    ar_digital_address VARCHAR(180),
    webhook_url VARCHAR(300),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_pref_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario (id)
);

CREATE INDEX IF NOT EXISTS idx_notif_pref_status ON tb_usuario_notification_preference (ativo);

ALTER TABLE notification_history ADD COLUMN IF NOT EXISTS tracking_token VARCHAR(120);
ALTER TABLE notification_history ADD COLUMN IF NOT EXISTS tracking_hash VARCHAR(128);
ALTER TABLE notification_history ADD COLUMN IF NOT EXISTS lido_em TIMESTAMPTZ;
ALTER TABLE notification_history ADD COLUMN IF NOT EXISTS ciencia_confirmada_em TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS idx_notification_history_tracking_token ON notification_history (tracking_token);
CREATE INDEX IF NOT EXISTS idx_notification_history_usuario_processo_canal ON notification_history (usuario_id, processo_id, canal);
CREATE INDEX IF NOT EXISTS idx_notification_history_status_enviado_em ON notification_history (status, enviado_em);
