CREATE TABLE adv_office_workspace_preference (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    preferred_equipe_id BIGINT,
    mode VARCHAR(16) NOT NULL DEFAULT 'PERSONAL',
    auto_activate_on_login BOOLEAN NOT NULL DEFAULT FALSE,
    allow_personal_own_cases BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_adv_office_workspace_preference_user UNIQUE (usuario_id),
    CONSTRAINT fk_adv_office_workspace_preference_user FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_adv_office_workspace_preference_equipe FOREIGN KEY (preferred_equipe_id) REFERENCES equipes(id) ON DELETE SET NULL
);

CREATE INDEX idx_adv_office_workspace_preference_equipe ON adv_office_workspace_preference(preferred_equipe_id);
