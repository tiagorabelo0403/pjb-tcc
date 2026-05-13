ALTER TABLE tb_recursal_mesh_aggregate
    ADD COLUMN IF NOT EXISTS integrity_fingerprint VARCHAR(64) DEFAULT repeat('0', 64);

UPDATE tb_recursal_mesh_aggregate
   SET integrity_fingerprint = repeat('0', 64)
 WHERE integrity_fingerprint IS NULL;

ALTER TABLE tb_recursal_mesh_aggregate
    ALTER COLUMN integrity_fingerprint SET NOT NULL;

ALTER TABLE tb_recursal_mesh_transition_ledger
    ADD COLUMN IF NOT EXISTS integrity_fingerprint VARCHAR(64) DEFAULT repeat('0', 64);

UPDATE tb_recursal_mesh_transition_ledger
   SET integrity_fingerprint = repeat('0', 64)
 WHERE integrity_fingerprint IS NULL;

ALTER TABLE tb_recursal_mesh_transition_ledger
    ALTER COLUMN integrity_fingerprint SET NOT NULL;

ALTER TABLE tb_recursal_mesh_process_projection
    ADD COLUMN IF NOT EXISTS integrity_fingerprint VARCHAR(64) DEFAULT repeat('0', 64);

UPDATE tb_recursal_mesh_process_projection
   SET integrity_fingerprint = repeat('0', 64)
 WHERE integrity_fingerprint IS NULL;

ALTER TABLE tb_recursal_mesh_process_projection
    ALTER COLUMN integrity_fingerprint SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_recursal_mesh_aggregate_integrity ON tb_recursal_mesh_aggregate(integrity_fingerprint);
CREATE INDEX IF NOT EXISTS idx_recursal_mesh_ledger_integrity ON tb_recursal_mesh_transition_ledger(integrity_fingerprint);
CREATE INDEX IF NOT EXISTS idx_recursal_mesh_projection_integrity ON tb_recursal_mesh_process_projection(integrity_fingerprint);
