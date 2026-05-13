ALTER TABLE security_challenges
    ADD COLUMN IF NOT EXISTS attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_attempt_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS strong_auth_usages (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    action_hash VARCHAR(64) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_sau_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id),
    CONSTRAINT uk_strong_auth_once UNIQUE (session_id, action_hash)
);

CREATE INDEX IF NOT EXISTS idx_sau_user ON strong_auth_usages(usuario_id, criado_em);
CREATE INDEX IF NOT EXISTS idx_sau_session ON strong_auth_usages(session_id);

CREATE TABLE IF NOT EXISTS dual_approval_requests (
    id BIGSERIAL PRIMARY KEY,
    requester_user_id BIGINT NOT NULL,
    requester_device_id BIGINT,
    equipe_id BIGINT,
    action VARCHAR(40) NOT NULL,
    method VARCHAR(12) NOT NULL,
    path VARCHAR(300) NOT NULL,
    rule_id VARCHAR(64),
    action_hash VARCHAR(64) NOT NULL,
    request_key VARCHAR(96) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    approved_by_user_id BIGINT,
    approved_at TIMESTAMP,
    rejected_by_user_id BIGINT,
    rejected_at TIMESTAMP,
    CONSTRAINT fk_dar_requester FOREIGN KEY (requester_user_id) REFERENCES tb_usuario(id),
    CONSTRAINT fk_dar_approver FOREIGN KEY (approved_by_user_id) REFERENCES tb_usuario(id),
    CONSTRAINT fk_dar_rejector FOREIGN KEY (rejected_by_user_id) REFERENCES tb_usuario(id),
    CONSTRAINT uk_dar_request_key UNIQUE (request_key)
);

CREATE INDEX IF NOT EXISTS idx_dar_status ON dual_approval_requests(status, expires_at);
CREATE INDEX IF NOT EXISTS idx_dar_user ON dual_approval_requests(requester_user_id, created_at);
