CREATE TABLE IF NOT EXISTS operational_function_credentials (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    function_code VARCHAR(80) NOT NULL,
    status VARCHAR(24) NOT NULL,
    secret_hash VARCHAR(255),
    justica_axis VARCHAR(32),
    tribunal_codigo VARCHAR(32),
    forum_code VARCHAR(96),
    unit_code VARCHAR(128),
    vara_label VARCHAR(160),
    uf VARCHAR(8),
    comarca VARCHAR(160),
    managed_by_user_id BIGINT,
    provisioned_by_user_id BIGINT,
    last_rotation_by_user_id BIGINT,
    reason VARCHAR(600),
    audit_trail_json TEXT,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP,
    activated_at TIMESTAMP,
    last_verified_at TIMESTAMP,
    last_reset_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_ofc_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id),
    CONSTRAINT fk_ofc_managed_by FOREIGN KEY (managed_by_user_id) REFERENCES tb_usuario(id),
    CONSTRAINT fk_ofc_provisioned_by FOREIGN KEY (provisioned_by_user_id) REFERENCES tb_usuario(id),
    CONSTRAINT fk_ofc_rotated_by FOREIGN KEY (last_rotation_by_user_id) REFERENCES tb_usuario(id),
    CONSTRAINT uk_ofc_usuario_function UNIQUE (usuario_id, function_code)
);

CREATE INDEX IF NOT EXISTS idx_ofc_status ON operational_function_credentials(status, updated_at);
CREATE INDEX IF NOT EXISTS idx_ofc_scope ON operational_function_credentials(function_code, tribunal_codigo, uf, comarca);
CREATE INDEX IF NOT EXISTS idx_ofc_user ON operational_function_credentials(usuario_id, function_code);

CREATE TABLE IF NOT EXISTS operational_function_unlock_sessions (
    id BIGSERIAL PRIMARY KEY,
    credential_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    function_code VARCHAR(80) NOT NULL,
    scope_action VARCHAR(80) NOT NULL,
    scope_reference VARCHAR(120) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    ip VARCHAR(64),
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ofus_credential FOREIGN KEY (credential_id) REFERENCES operational_function_credentials(id),
    CONSTRAINT fk_ofus_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id),
    CONSTRAINT uk_ofus_token UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_ofus_user ON operational_function_unlock_sessions(usuario_id, function_code, expires_at);
CREATE INDEX IF NOT EXISTS idx_ofus_scope ON operational_function_unlock_sessions(function_code, scope_action, scope_reference, expires_at);
CREATE INDEX IF NOT EXISTS idx_ofus_expires ON operational_function_unlock_sessions(expires_at);
