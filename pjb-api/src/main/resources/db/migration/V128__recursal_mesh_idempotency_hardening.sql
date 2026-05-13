ALTER TABLE tb_recursal_mesh_transition_ledger
    ADD COLUMN IF NOT EXISTS command_id VARCHAR(160),
    ADD COLUMN IF NOT EXISTS context_json TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS idx_recursal_mesh_ledger_recurso_command
    ON tb_recursal_mesh_transition_ledger (recurso_id, command_id)
    WHERE command_id IS NOT NULL;
