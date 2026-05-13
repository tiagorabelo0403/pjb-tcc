-- Complemento: coluna updated_at na delegação de prazos
ALTER TABLE IF EXISTS tb_laiane_deadline_delegation
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_laiane_deadline_delegation_updated_at
    ON tb_laiane_deadline_delegation(updated_at);
