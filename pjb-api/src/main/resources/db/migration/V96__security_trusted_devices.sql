
CREATE TABLE IF NOT EXISTS user_security_profile (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    last_login_ip VARCHAR(64),
    last_login_network VARCHAR(120),
    last_login_at TIMESTAMP,
    last_strong_auth_at TIMESTAMP,
    criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_security_profile UNIQUE (usuario_id),
    CONSTRAINT fk_usp_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id)
);

CREATE INDEX IF NOT EXISTS idx_usp_user ON user_security_profile(usuario_id);

CREATE TABLE IF NOT EXISTS trusted_devices (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    credential_id VARCHAR(512) NOT NULL,
    public_key VARCHAR(4096) NOT NULL,
    alias VARCHAR(80) NOT NULL,
    aaguid VARCHAR(64),
    attestation_fmt VARCHAR(40),
    attestation_trusted BOOLEAN NOT NULL DEFAULT FALSE,
    enroll_ip VARCHAR(64),
    enroll_network VARCHAR(120),
    enroll_suspect_network BOOLEAN NOT NULL DEFAULT FALSE,
    risk_score_enroll INT NOT NULL DEFAULT 0,
    quarentena_ate TIMESTAMP,
    criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    ultimo_uso_em TIMESTAMP,
    revogado_em TIMESTAMP,
    CONSTRAINT fk_device_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id),
    CONSTRAINT uk_device_credential UNIQUE (credential_id),
    CONSTRAINT uk_user_device_alias UNIQUE (usuario_id, alias)
);

CREATE INDEX IF NOT EXISTS idx_device_user ON trusted_devices(usuario_id);
CREATE INDEX IF NOT EXISTS idx_device_active ON trusted_devices(usuario_id, revogado_em);

CREATE TABLE IF NOT EXISTS security_alerts (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT,
    tipo VARCHAR(60) NOT NULL,
    titulo VARCHAR(180) NOT NULL,
    detalhes VARCHAR(2000),
    ip VARCHAR(64),
    risk_score INT NOT NULL DEFAULT 0,
    ack BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_alert_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id)
);

CREATE INDEX IF NOT EXISTS idx_alert_user ON security_alerts(usuario_id, criado_em);
CREATE INDEX IF NOT EXISTS idx_alert_type ON security_alerts(tipo, criado_em);
