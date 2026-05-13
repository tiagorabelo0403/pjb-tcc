CREATE TABLE IF NOT EXISTS tb_secretariat_queue_item (
    work_item_id BIGINT PRIMARY KEY,
    processo_id BIGINT NOT NULL,
    inbox_key VARCHAR(120) NOT NULL,
    queue_code VARCHAR(120),
    status VARCHAR(30) NOT NULL,
    prioridade INT NOT NULL,
    due_at TIMESTAMPTZ,
    score INT NOT NULL,
    tags_json TEXT,
    titulo VARCHAR(220) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sec_queue_inbox_status ON tb_secretariat_queue_item (inbox_key, status);
CREATE INDEX IF NOT EXISTS idx_sec_queue_due ON tb_secretariat_queue_item (due_at);
CREATE INDEX IF NOT EXISTS idx_sec_queue_priority ON tb_secretariat_queue_item (prioridade);
CREATE INDEX IF NOT EXISTS idx_sec_queue_score ON tb_secretariat_queue_item (score);
