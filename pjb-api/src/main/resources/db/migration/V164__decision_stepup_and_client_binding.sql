ALTER TABLE tb_decision_focus_session
    ADD COLUMN IF NOT EXISTS tab_binding VARCHAR(160),
    ADD COLUMN IF NOT EXISTS route_binding VARCHAR(240),
    ADD COLUMN IF NOT EXISTS binding_fingerprint VARCHAR(128),
    ADD COLUMN IF NOT EXISTS last_heartbeat_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_decision_focus_binding_heartbeat
    ON tb_decision_focus_session (usuario_id, status, last_heartbeat_at DESC);

CREATE TABLE IF NOT EXISTS tb_decision_stepup_consumption (
    id BIGSERIAL PRIMARY KEY,
    token_jti VARCHAR(120) NOT NULL UNIQUE,
    act_type VARCHAR(60) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    processo_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    focus_session_id BIGINT,
    consumed_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_decision_stepup_processo FOREIGN KEY (processo_id) REFERENCES tb_processo(id),
    CONSTRAINT fk_decision_stepup_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario (id),
    CONSTRAINT fk_decision_stepup_focus FOREIGN KEY (focus_session_id) REFERENCES tb_decision_focus_session(id)
);

CREATE INDEX IF NOT EXISTS idx_decision_stepup_user_created
    ON tb_decision_stepup_consumption (usuario_id, consumed_at DESC);

CREATE INDEX IF NOT EXISTS idx_decision_stepup_process_created
    ON tb_decision_stepup_consumption (processo_id, consumed_at DESC);
