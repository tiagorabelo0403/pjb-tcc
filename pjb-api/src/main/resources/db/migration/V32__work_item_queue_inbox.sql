-- PJB 2.0 (Kernel Recursal) — filas/localizadores institucionais em WorkItem.
-- Objetivo: permitir inbox por "localizador" (triagem, distribuição, gabinete prevento virtual) sem quebrar compatibilidade.

ALTER TABLE tb_work_item
    ADD COLUMN IF NOT EXISTS queue_code VARCHAR(120);

ALTER TABLE tb_work_item
    ADD COLUMN IF NOT EXISTS inbox_key VARCHAR(120);

CREATE INDEX IF NOT EXISTS idx_workitem_queue_status
    ON tb_work_item (queue_code, status);

CREATE INDEX IF NOT EXISTS idx_workitem_inbox_status
    ON tb_work_item (inbox_key, status);
