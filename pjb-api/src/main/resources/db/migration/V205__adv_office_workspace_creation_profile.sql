CREATE TABLE IF NOT EXISTS adv_office_workspace_profile (
    id BIGSERIAL PRIMARY KEY,
    equipe_id BIGINT NOT NULL UNIQUE REFERENCES equipes(id) ON DELETE CASCADE,
    owner_user_id BIGINT NOT NULL REFERENCES tb_usuario(id) ON DELETE RESTRICT,
    display_name VARCHAR(150) NOT NULL,
    all_brazilian_law_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_adv_office_workspace_profile_owner ON adv_office_workspace_profile(owner_user_id);
