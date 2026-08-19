ALTER TABLE passkey_sessions ADD COLUMN last_seen_at TIMESTAMP;
UPDATE passkey_sessions SET last_seen_at = criado_em WHERE last_seen_at IS NULL;
