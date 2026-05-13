CREATE TABLE IF NOT EXISTS tb_user_calendar_preference (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    visible_lane_codes VARCHAR(500),
    pinned_lane_codes VARCHAR(500),
    hidden_lane_codes VARCHAR(500),
    default_view VARCHAR(20),
    include_personal_calendar BOOLEAN NOT NULL DEFAULT TRUE,
    include_institutional_calendar BOOLEAN NOT NULL DEFAULT FALSE,
    highlight_urgent_days BOOLEAN NOT NULL DEFAULT TRUE,
    selected_scope_code VARCHAR(64),
    selected_team_id BIGINT,
    selected_institution_context_code VARCHAR(64),
    notification_cadence_mode VARCHAR(20),
    notification_lane_codes VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_ucp_user ON tb_user_calendar_preference (usuario_id);

ALTER TABLE tb_user_calendar_preference
    ADD COLUMN IF NOT EXISTS visible_lane_codes VARCHAR(500),
    ADD COLUMN IF NOT EXISTS pinned_lane_codes VARCHAR(500),
    ADD COLUMN IF NOT EXISTS hidden_lane_codes VARCHAR(500),
    ADD COLUMN IF NOT EXISTS default_view VARCHAR(20),
    ADD COLUMN IF NOT EXISTS include_personal_calendar BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS include_institutional_calendar BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS highlight_urgent_days BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS selected_scope_code VARCHAR(64),
    ADD COLUMN IF NOT EXISTS selected_team_id BIGINT,
    ADD COLUMN IF NOT EXISTS selected_institution_context_code VARCHAR(64),
    ADD COLUMN IF NOT EXISTS notification_cadence_mode VARCHAR(20),
    ADD COLUMN IF NOT EXISTS notification_lane_codes VARCHAR(500),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();
