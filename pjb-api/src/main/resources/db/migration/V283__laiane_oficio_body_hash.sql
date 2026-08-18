ALTER TABLE tb_laiane_oficio ADD COLUMN IF NOT EXISTS body_hash VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_laiane_oficio_body_hash ON tb_laiane_oficio(body_hash);
