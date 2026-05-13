ALTER TABLE tb_atendimento_message ALTER COLUMN status TYPE VARCHAR(24);
ALTER TABLE tb_atendimento_message ADD COLUMN IF NOT EXISTS blocked_reason VARCHAR(64);
ALTER TABLE tb_atendimento_message ADD COLUMN IF NOT EXISTS blocked_note VARCHAR(200);
ALTER TABLE tb_atendimento_message ADD COLUMN IF NOT EXISTS blocked_at TIMESTAMPTZ;
ALTER TABLE tb_atendimento_message ADD COLUMN IF NOT EXISTS blocked_by_user_id BIGINT;
