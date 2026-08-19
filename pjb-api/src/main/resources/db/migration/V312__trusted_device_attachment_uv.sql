ALTER TABLE trusted_devices ADD COLUMN authenticator_attachment VARCHAR(20);
ALTER TABLE trusted_devices ADD COLUMN user_verified BOOLEAN NOT NULL DEFAULT FALSE;
