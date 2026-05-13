CREATE TABLE IF NOT EXISTS security_challenges (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT,
    tipo VARCHAR(40) NOT NULL,
    code_hash VARCHAR(64),
    nonce VARCHAR(512),
    details VARCHAR(1200),
    ip VARCHAR(64),
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP,
    criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_sc_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id)
);

CREATE INDEX IF NOT EXISTS idx_sc_user ON security_challenges(usuario_id, expires_at);
CREATE INDEX IF NOT EXISTS idx_sc_expires ON security_challenges(expires_at);

CREATE TABLE IF NOT EXISTS webauthn_sessions (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT,
    tipo VARCHAR(24) NOT NULL,
    request_json VARCHAR(12000) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_webauthn_session_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id)
);

CREATE INDEX IF NOT EXISTS idx_webauthn_session_user ON webauthn_sessions(usuario_id, expires_at);
CREATE INDEX IF NOT EXISTS idx_webauthn_session_expires ON webauthn_sessions(expires_at);

CREATE TABLE IF NOT EXISTS passkey_sessions (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    device_id BIGINT,
    token_hash VARCHAR(64) NOT NULL,
    ip VARCHAR(64),
    expires_at TIMESTAMP NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    revogado_em TIMESTAMP,
    CONSTRAINT fk_passkey_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id),
    CONSTRAINT uk_passkey_token UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_passkey_user ON passkey_sessions(usuario_id, expires_at);
CREATE INDEX IF NOT EXISTS idx_passkey_expires ON passkey_sessions(expires_at);

ALTER TABLE user_security_profile
    ADD COLUMN IF NOT EXISTS adv_baptized_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS adv_baptism_method VARCHAR(40),
    ADD COLUMN IF NOT EXISTS adv_baptism_cert_fingerprint VARCHAR(96),
    ADD COLUMN IF NOT EXISTS totp_secret VARCHAR(120),
    ADD COLUMN IF NOT EXISTS totp_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE trusted_devices
    ADD COLUMN IF NOT EXISTS sign_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS pending_challenge_id BIGINT,
    ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_device_pending ON trusted_devices(pending_challenge_id);
