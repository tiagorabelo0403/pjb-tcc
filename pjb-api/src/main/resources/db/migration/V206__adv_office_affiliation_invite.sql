CREATE TABLE adv_office_affiliation_invite (
    id BIGSERIAL PRIMARY KEY,
    equipe_id BIGINT NOT NULL REFERENCES equipes(id) ON DELETE CASCADE,
    created_by_user_id BIGINT NOT NULL REFERENCES tb_usuario(id) ON DELETE RESTRICT,
    target_user_id BIGINT REFERENCES tb_usuario(id) ON DELETE SET NULL,
    invited_nome VARCHAR(150),
    invited_email VARCHAR(200),
    invited_cpf VARCHAR(11),
    invited_oab VARCHAR(64),
    papel_equipe VARCHAR(40) NOT NULL,
    cargo VARCHAR(120),
    allow_all_ramos BOOLEAN NOT NULL DEFAULT TRUE,
    allowed_ramos_override VARCHAR(1200) NOT NULL DEFAULT '',
    min_trust_for_auto INT,
    max_auto_por_dia INT,
    block_personal_cases BOOLEAN NOT NULL DEFAULT FALSE,
    auto_activate_on_accept BOOLEAN NOT NULL DEFAULT TRUE,
    mode_on_accept VARCHAR(20) NOT NULL DEFAULT 'HYBRID',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    responded_at TIMESTAMPTZ,
    response_by_user_id BIGINT REFERENCES tb_usuario(id) ON DELETE SET NULL
);

CREATE INDEX ix_adv_office_affiliation_invite_equipe ON adv_office_affiliation_invite (equipe_id);
CREATE INDEX ix_adv_office_affiliation_invite_status ON adv_office_affiliation_invite (status);
CREATE INDEX ix_adv_office_affiliation_invite_target_user ON adv_office_affiliation_invite (target_user_id);
