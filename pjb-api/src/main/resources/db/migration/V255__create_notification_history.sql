CREATE TABLE IF NOT EXISTS notification_history (
    id                   BIGSERIAL    PRIMARY KEY,
    usuario_id           BIGINT,
    processo_id          BIGINT,
    canal                VARCHAR(50),
    titulo               VARCHAR(255),
    mensagem             TEXT,
    enviado_em           TIMESTAMP,
    status               VARCHAR(30),
    tracking_token       VARCHAR(120) UNIQUE,
    tracking_hash        VARCHAR(128),
    lido_em              TIMESTAMP,
    ciencia_confirmada_em TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_user_time
    ON notification_history (usuario_id, enviado_em);

CREATE INDEX IF NOT EXISTS idx_notification_user_status_time
    ON notification_history (usuario_id, status, enviado_em);

CREATE INDEX IF NOT EXISTS idx_notification_user_unread
    ON notification_history (usuario_id, lido_em, enviado_em);

CREATE INDEX IF NOT EXISTS idx_notification_process_title
    ON notification_history (processo_id, titulo);

CREATE INDEX IF NOT EXISTS idx_notification_user_channel_title_time
    ON notification_history (usuario_id, canal, titulo, enviado_em);

CREATE INDEX IF NOT EXISTS idx_notification_user_process_channel_time
    ON notification_history (usuario_id, processo_id, canal, enviado_em);

CREATE INDEX IF NOT EXISTS idx_notification_user_process_ciencia_time
    ON notification_history (usuario_id, processo_id, ciencia_confirmada_em, enviado_em);

CREATE INDEX IF NOT EXISTS idx_notification_history_tracking_token
    ON notification_history (tracking_token);

CREATE INDEX IF NOT EXISTS idx_notification_history_tracking_usuario
    ON notification_history (tracking_token, usuario_id);

CREATE INDEX IF NOT EXISTS idx_notification_history_usuario_processo_canal
    ON notification_history (usuario_id, processo_id, canal);

CREATE INDEX IF NOT EXISTS idx_notification_history_status_enviado_em
    ON notification_history (status, enviado_em);
