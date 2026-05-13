CREATE TABLE IF NOT EXISTS tb_cidadao_doc_vault_item (
  id UUID PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  doc_key VARCHAR(80) NOT NULL,
  title VARCHAR(180) NOT NULL,
  source VARCHAR(16) NOT NULL,
  origin_url TEXT NULL,
  storage_key TEXT NULL,
  content_type VARCHAR(80) NULL,
  size_bytes BIGINT NULL,
  sha256 VARCHAR(64) NULL,
  issued_at DATE NULL,
  expires_at DATE NULL,
  notes TEXT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_cidvault_user_updated ON tb_cidadao_doc_vault_item (usuario_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS ix_cidvault_user_dockey ON tb_cidadao_doc_vault_item (usuario_id, doc_key);

CREATE TABLE IF NOT EXISTS tb_carteira_share (
  id UUID PRIMARY KEY,
  owner_usuario_id BIGINT NOT NULL,
  processo_id BIGINT NULL,
  audience VARCHAR(16) NOT NULL,
  oab_normalizada VARCHAR(80) NULL,
  revoked BOOLEAN NOT NULL DEFAULT false,
  expires_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_cartshare_owner ON tb_carteira_share (owner_usuario_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_cartshare_exp ON tb_carteira_share (expires_at);

CREATE TABLE IF NOT EXISTS tb_carteira_share_item (
  share_id UUID NOT NULL,
  vault_item_id UUID NOT NULL,
  PRIMARY KEY (share_id, vault_item_id)
);

CREATE INDEX IF NOT EXISTS ix_cartshareitem_share ON tb_carteira_share_item (share_id);
