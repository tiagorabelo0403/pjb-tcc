-- Hardening: invariantes de estado do checklist + regra temporal básica
-- Garante que o checklist não exista em estado incoerente mesmo com bugs futuros.

-- 1) Backfill/normalização (dados antigos)
UPDATE tb_atendimento_checklist_item
SET
  created_at = COALESCE(created_at, NOW()),
  updated_at = COALESCE(updated_at, created_at, NOW())
WHERE created_at IS NULL OR updated_at IS NULL;

-- OPEN: não pode ter campos de conclusão/cancelamento
UPDATE tb_atendimento_checklist_item
SET
  completed_at = NULL,
  completed_by_user_id = NULL,
  cancelled_at = NULL,
  cancelled_by_user_id = NULL
WHERE status = 'OPEN'
  AND (completed_at IS NOT NULL OR completed_by_user_id IS NOT NULL OR cancelled_at IS NOT NULL OR cancelled_by_user_id IS NOT NULL);

-- DONE: exige completed* e não pode ter cancelled*
UPDATE tb_atendimento_checklist_item
SET
  completed_at = COALESCE(completed_at, updated_at, created_at, NOW()),
  completed_by_user_id = COALESCE(completed_by_user_id, created_by_user_id),
  cancelled_at = NULL,
  cancelled_by_user_id = NULL
WHERE status = 'DONE'
  AND (completed_at IS NULL OR completed_by_user_id IS NULL OR cancelled_at IS NOT NULL OR cancelled_by_user_id IS NOT NULL);

-- CANCELLED: exige cancelled* e não pode ter completed*
UPDATE tb_atendimento_checklist_item
SET
  cancelled_at = COALESCE(cancelled_at, updated_at, created_at, NOW()),
  cancelled_by_user_id = COALESCE(cancelled_by_user_id, created_by_user_id),
  completed_at = NULL,
  completed_by_user_id = NULL
WHERE status = 'CANCELLED'
  AND (cancelled_at IS NULL OR cancelled_by_user_id IS NULL OR completed_at IS NOT NULL OR completed_by_user_id IS NOT NULL);

-- 2) CHECK constraints (Postgres não tem IF NOT EXISTS nativo aqui, então usamos pg_constraint)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_atendimento_checklist_item_state'
  ) THEN
    ALTER TABLE tb_atendimento_checklist_item
      ADD CONSTRAINT chk_atendimento_checklist_item_state
      CHECK (
        (status = 'OPEN'
          AND completed_at IS NULL AND completed_by_user_id IS NULL
          AND cancelled_at IS NULL AND cancelled_by_user_id IS NULL)
        OR
        (status = 'DONE'
          AND completed_at IS NOT NULL AND completed_by_user_id IS NOT NULL
          AND cancelled_at IS NULL AND cancelled_by_user_id IS NULL)
        OR
        (status = 'CANCELLED'
          AND cancelled_at IS NOT NULL AND cancelled_by_user_id IS NOT NULL
          AND completed_at IS NULL AND completed_by_user_id IS NULL)
      );
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_atendimento_checklist_item_temporal'
  ) THEN
    ALTER TABLE tb_atendimento_checklist_item
      ADD CONSTRAINT chk_atendimento_checklist_item_temporal
      CHECK (updated_at >= created_at);
  END IF;
END $$;
