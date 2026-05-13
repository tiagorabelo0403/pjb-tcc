CREATE TABLE IF NOT EXISTS tb_ui_state_history (
    id UUID PRIMARY KEY,
    subject_type VARCHAR(30) NOT NULL,
    processo_id BIGINT,
    work_item_id BIGINT,
    inbox_key VARCHAR(180),
    event_type VARCHAR(120) NOT NULL,
    from_status VARCHAR(80),
    to_status VARCHAR(80),
    from_tokens_json TEXT,
    to_tokens_json TEXT,
    actor_user_id BIGINT,
    actor_role VARCHAR(60),
    message TEXT,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ui_hist_proc_time ON tb_ui_state_history (processo_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_ui_hist_work_time ON tb_ui_state_history (work_item_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_ui_hist_inbox_time ON tb_ui_state_history (inbox_key, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_ui_hist_type_time ON tb_ui_state_history (subject_type, occurred_at DESC);
