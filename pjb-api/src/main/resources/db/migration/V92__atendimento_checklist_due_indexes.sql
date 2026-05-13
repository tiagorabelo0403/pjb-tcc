-- Índices adicionais para queries leves do checklist (overdue/nextDue)
-- Otimiza agregações por thread/status/due_at usadas em lista de threads e digest.

CREATE INDEX IF NOT EXISTS idx_atendimento_chk_thread_status_due
  ON tb_atendimento_checklist_item (thread_id, status, due_at);

CREATE INDEX IF NOT EXISTS idx_atendimento_chk_thread_due
  ON tb_atendimento_checklist_item (thread_id, due_at);
