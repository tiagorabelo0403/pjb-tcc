CREATE TABLE IF NOT EXISTS tb_usuario_avatar (
  usuario_id BIGINT PRIMARY KEY,
  storage_key TEXT NOT NULL,
  content_type VARCHAR(80) NOT NULL,
  size_bytes BIGINT NOT NULL,
  sha256 VARCHAR(64) NOT NULL,
  source VARCHAR(20) NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_usuario_avatar_sha256 ON tb_usuario_avatar (sha256);
