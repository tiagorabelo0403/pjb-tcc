CREATE TABLE IF NOT EXISTS tb_govbr_link_state (
  state_id UUID PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  cpf VARCHAR(11) NOT NULL,
  code_verifier VARCHAR(128) NOT NULL,
  nonce VARCHAR(96) NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  used_at TIMESTAMPTZ NULL,
  created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_govbr_link_state_user ON tb_govbr_link_state (usuario_id, expires_at);
CREATE INDEX IF NOT EXISTS ix_govbr_link_state_expires ON tb_govbr_link_state (expires_at);
