CREATE TABLE IF NOT EXISTS tb_decision_focus_session (
    id BIGSERIAL PRIMARY KEY,
    processo_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    session_token VARCHAR(120) NOT NULL UNIQUE,
    process_fingerprint VARCHAR(128) NOT NULL,
    status VARCHAR(30) NOT NULL,
    window_binding VARCHAR(160),
    numero_snapshot VARCHAR(60),
    classe_snapshot VARCHAR(160),
    autor_snapshot VARCHAR(220),
    reu_snapshot VARCHAR(220),
    assunto_snapshot VARCHAR(240),
    summary_snapshot VARCHAR(600),
    opened_at TIMESTAMP NOT NULL,
    armed_at TIMESTAMP,
    last_checked_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    released_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_decision_focus_processo FOREIGN KEY (processo_id) REFERENCES tb_processo(id),
    CONSTRAINT fk_decision_focus_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id)
);

CREATE INDEX IF NOT EXISTS idx_decision_focus_user_status
    ON tb_decision_focus_session (usuario_id, status, opened_at DESC);

CREATE INDEX IF NOT EXISTS idx_decision_focus_process_status
    ON tb_decision_focus_session (processo_id, status, opened_at DESC);

CREATE TABLE IF NOT EXISTS tb_decision_confusion_audit (
    id BIGSERIAL PRIMARY KEY,
    processo_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    focus_session_id BIGINT,
    act_type VARCHAR(50) NOT NULL,
    target_process_fingerprint VARCHAR(128) NOT NULL,
    request_text_hash VARCHAR(128) NOT NULL,
    result_status VARCHAR(30) NOT NULL,
    semantic_score INT,
    competing_score INT,
    competing_processo_id BIGINT,
    reasons_json TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_decision_confusion_processo FOREIGN KEY (processo_id) REFERENCES tb_processo(id),
    CONSTRAINT fk_decision_confusion_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id),
    CONSTRAINT fk_decision_confusion_focus FOREIGN KEY (focus_session_id) REFERENCES tb_decision_focus_session(id)
);

CREATE INDEX IF NOT EXISTS idx_decision_confusion_process_created
    ON tb_decision_confusion_audit (processo_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_decision_confusion_user_created
    ON tb_decision_confusion_audit (usuario_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_decision_confusion_status
    ON tb_decision_confusion_audit (result_status, created_at DESC);
