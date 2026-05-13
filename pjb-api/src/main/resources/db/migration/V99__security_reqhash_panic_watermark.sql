ALTER TABLE passkey_sessions
    ADD COLUMN IF NOT EXISTS scope_action VARCHAR(40),
    ADD COLUMN IF NOT EXISTS scope_request_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS scope_one_shot BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_passkey_scope ON passkey_sessions(usuario_id, scope_action, scope_one_shot, expires_at);

ALTER TABLE strong_auth_usages
    ADD COLUMN IF NOT EXISTS request_hash VARCHAR(64) NOT NULL DEFAULT '';

UPDATE strong_auth_usages SET request_hash = action_hash WHERE request_hash = '';

ALTER TABLE strong_auth_usages
    DROP CONSTRAINT IF EXISTS uk_strong_auth_once;

ALTER TABLE strong_auth_usages
    ADD CONSTRAINT uk_strong_auth_once_req UNIQUE (session_id, request_hash);

CREATE INDEX IF NOT EXISTS idx_sau_reqhash ON strong_auth_usages(request_hash, criado_em);

ALTER TABLE user_security_profile
    ADD COLUMN IF NOT EXISTS frozen_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS frozen_until TIMESTAMP,
    ADD COLUMN IF NOT EXISTS freeze_reason VARCHAR(200);

CREATE TABLE IF NOT EXISTS download_events (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    device_id BIGINT,
    path VARCHAR(300) NOT NULL,
    bytes BIGINT NOT NULL,
    watermark_id VARCHAR(96),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_de_user FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id)
);

CREATE INDEX IF NOT EXISTS idx_de_user_time ON download_events(usuario_id, created_at);
CREATE INDEX IF NOT EXISTS idx_de_device_time ON download_events(device_id, created_at);
