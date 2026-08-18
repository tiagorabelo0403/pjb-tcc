CREATE TABLE IF NOT EXISTS tb_govbr_login_state (
  state_id UUID PRIMARY KEY,
  code_verifier VARCHAR(128) NOT NULL,
  nonce VARCHAR(96) NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  used_at TIMESTAMPTZ NULL,
  usuario_id BIGINT NULL,
  session_retrieved_at TIMESTAMPTZ NULL,
  created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_govbr_login_state_expires ON tb_govbr_login_state (expires_at);
