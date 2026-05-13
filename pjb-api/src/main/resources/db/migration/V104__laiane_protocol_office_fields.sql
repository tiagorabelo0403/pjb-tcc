ALTER TABLE tb_laiane_protocol_package ADD COLUMN IF NOT EXISTS equipe_id BIGINT;
ALTER TABLE tb_laiane_protocol_package ADD COLUMN IF NOT EXISTS executor_user_id BIGINT;
ALTER TABLE tb_laiane_protocol_package ADD COLUMN IF NOT EXISTS signer_user_id BIGINT;
ALTER TABLE tb_laiane_protocol_package ADD COLUMN IF NOT EXISTS office_queue_item_id BIGINT;
ALTER TABLE tb_laiane_protocol_package ADD COLUMN IF NOT EXISTS submission_job_id UUID;
ALTER TABLE tb_laiane_protocol_package ADD COLUMN IF NOT EXISTS external_protocol_ref VARCHAR(120);
ALTER TABLE tb_laiane_protocol_package ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMPTZ;
ALTER TABLE tb_laiane_protocol_package ADD COLUMN IF NOT EXISTS last_error TEXT;

DO $$ BEGIN
  ALTER TABLE tb_laiane_protocol_package
    ADD CONSTRAINT fk_laiane_protocol_equipe FOREIGN KEY (equipe_id) REFERENCES equipes(id);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE tb_laiane_protocol_package
    ADD CONSTRAINT fk_laiane_protocol_executor FOREIGN KEY (executor_user_id) REFERENCES tb_usuario(id);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE tb_laiane_protocol_package
    ADD CONSTRAINT fk_laiane_protocol_signer FOREIGN KEY (signer_user_id) REFERENCES tb_usuario(id);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE tb_laiane_protocol_package
    ADD CONSTRAINT fk_laiane_protocol_queue_item FOREIGN KEY (office_queue_item_id) REFERENCES adv_office_signature_queue(id);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE tb_laiane_protocol_package
    ADD CONSTRAINT fk_laiane_protocol_job FOREIGN KEY (submission_job_id) REFERENCES tb_job(id);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

CREATE INDEX IF NOT EXISTS ix_laiane_protocol_equipe_created ON tb_laiane_protocol_package(equipe_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_laiane_protocol_signer_status ON tb_laiane_protocol_package(signer_user_id, status, updated_at DESC);
CREATE INDEX IF NOT EXISTS ix_laiane_protocol_status_updated ON tb_laiane_protocol_package(status, updated_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uk_laiane_protocol_queue_item ON tb_laiane_protocol_package(office_queue_item_id) WHERE office_queue_item_id IS NOT NULL;
