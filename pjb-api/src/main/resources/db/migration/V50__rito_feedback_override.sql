-- Catálogo de correção/feedback de rito (admin) para melhoria contínua sem redeploy.
-- Objetivo:
-- 1) Persistir feedback humano quando o classificador tiver baixa confiança.
-- 2) Permitir override por processo (com auditoria) para estabilizar UX imediatamente.

CREATE TABLE IF NOT EXISTS tb_rito_feedback (
    id UUID PRIMARY KEY,
    processo_id BIGINT NOT NULL,
    rito_resolved VARCHAR(64),
    rito_chosen VARCHAR(64) NOT NULL,
    confidence NUMERIC(5,4),
    reasons_json TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by_user_id BIGINT
);

CREATE INDEX IF NOT EXISTS idx_tb_rito_feedback_processo_id
    ON tb_rito_feedback (processo_id);

CREATE INDEX IF NOT EXISTS idx_tb_rito_feedback_created_at
    ON tb_rito_feedback (created_at);

-- Override de rito por processo: usado pelo motor de resolução antes de qualquer inferência.
CREATE TABLE IF NOT EXISTS tb_rito_override (
    processo_id BIGINT PRIMARY KEY,
    rito_code VARCHAR(64) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by_user_id BIGINT
);

CREATE INDEX IF NOT EXISTS idx_tb_rito_override_rito_code
    ON tb_rito_override (rito_code);
