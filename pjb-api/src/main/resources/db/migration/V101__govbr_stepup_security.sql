ALTER TABLE IF EXISTS user_security_profile
  ADD COLUMN IF NOT EXISTS gov_verified_at TIMESTAMPTZ NULL,
  ADD COLUMN IF NOT EXISTS gov_verified_sub_hash VARCHAR(64) NULL,
  ADD COLUMN IF NOT EXISTS gov_email_hash VARCHAR(64) NULL,
  ADD COLUMN IF NOT EXISTS gov_phone_hash VARCHAR(64) NULL,
  ADD COLUMN IF NOT EXISTS gov_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS gov_phone_verified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS tb_govbr_stepup_state (
  state_id UUID PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  cpf VARCHAR(11) NOT NULL,
  device_id BIGINT NULL,
  code_verifier VARCHAR(128) NOT NULL,
  nonce VARCHAR(96) NOT NULL,
  scope VARCHAR(200) NOT NULL,
  request_ip VARCHAR(64) NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  used_at TIMESTAMPTZ NULL,
  created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_govbr_stepup_state_user ON tb_govbr_stepup_state (usuario_id, expires_at);
CREATE INDEX IF NOT EXISTS ix_govbr_stepup_state_expires ON tb_govbr_stepup_state (expires_at);
