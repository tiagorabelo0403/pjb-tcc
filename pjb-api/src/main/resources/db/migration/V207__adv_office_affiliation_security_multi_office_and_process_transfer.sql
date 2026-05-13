ALTER TABLE adv_office_affiliation_invite
    ADD COLUMN IF NOT EXISTS workspace_priority INT NOT NULL DEFAULT 100,
    ADD COLUMN IF NOT EXISTS requires_final_approval BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS required_assurance_level VARCHAR(16),
    ADD COLUMN IF NOT EXISTS invite_payload_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS recipient_identity_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS invite_version INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS accepted_mode VARCHAR(20),
    ADD COLUMN IF NOT EXISTS accepted_auto_activate_on_login BOOLEAN,
    ADD COLUMN IF NOT EXISTS accepted_allow_personal_own_cases BOOLEAN,
    ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS accepted_by_user_id BIGINT REFERENCES tb_usuario(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS final_approved_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS final_approved_by_user_id BIGINT REFERENCES tb_usuario(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS final_approval_deadline_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS final_approval_justification VARCHAR(2000);

ALTER TABLE adv_office_delegacao_regra
    ADD COLUMN IF NOT EXISTS workspace_priority INT NOT NULL DEFAULT 100,
    ADD COLUMN IF NOT EXISTS auto_activate_workspace BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS adv_office_process_transfer (
    id BIGSERIAL PRIMARY KEY,
    source_equipe_id BIGINT NOT NULL REFERENCES equipes(id) ON DELETE CASCADE,
    target_equipe_id BIGINT NOT NULL REFERENCES equipes(id) ON DELETE CASCADE,
    initiated_by_user_id BIGINT NOT NULL REFERENCES tb_usuario(id) ON DELETE RESTRICT,
    source_responsible_user_id BIGINT REFERENCES tb_usuario(id) ON DELETE SET NULL,
    target_responsible_user_id BIGINT NOT NULL REFERENCES tb_usuario(id) ON DELETE RESTRICT,
    process_count INT NOT NULL DEFAULT 0,
    sensitive_process_count INT NOT NULL DEFAULT 0,
    motivo VARCHAR(2000),
    escopo VARCHAR(500),
    impact_hash VARCHAR(128),
    preview_summary VARCHAR(4000),
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING_DESTINATION_ACCEPTANCE',
    created_at TIMESTAMPTZ NOT NULL,
    responded_at TIMESTAMPTZ,
    response_by_user_id BIGINT REFERENCES tb_usuario(id) ON DELETE SET NULL,
    executed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS ix_adv_office_process_transfer_source ON adv_office_process_transfer(source_equipe_id);
CREATE INDEX IF NOT EXISTS ix_adv_office_process_transfer_target ON adv_office_process_transfer(target_equipe_id);
CREATE INDEX IF NOT EXISTS ix_adv_office_process_transfer_status ON adv_office_process_transfer(status);
CREATE INDEX IF NOT EXISTS ix_adv_office_process_transfer_target_user ON adv_office_process_transfer(target_responsible_user_id);

CREATE TABLE IF NOT EXISTS adv_office_process_transfer_item (
    id BIGSERIAL PRIMARY KEY,
    transfer_id BIGINT NOT NULL REFERENCES adv_office_process_transfer(id) ON DELETE CASCADE,
    processo_id BIGINT NOT NULL REFERENCES tb_processo(id) ON DELETE CASCADE,
    source_equipe_id BIGINT,
    source_usuario_id BIGINT,
    target_equipe_id BIGINT NOT NULL,
    target_usuario_id BIGINT NOT NULL,
    ramo_direito VARCHAR(64),
    nivel_sigilo VARCHAR(64),
    numero_processo_snapshot VARCHAR(64),
    CONSTRAINT uk_adv_office_process_transfer_item UNIQUE (transfer_id, processo_id)
);

CREATE INDEX IF NOT EXISTS ix_adv_office_process_transfer_item_transfer ON adv_office_process_transfer_item(transfer_id);
CREATE INDEX IF NOT EXISTS ix_adv_office_process_transfer_item_processo ON adv_office_process_transfer_item(processo_id);
