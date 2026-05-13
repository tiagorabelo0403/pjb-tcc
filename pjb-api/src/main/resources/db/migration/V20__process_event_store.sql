-- PJB v26 - Kernel Processual: Event Store (auditável e idempotente)

CREATE TABLE IF NOT EXISTS tb_processo_event (
    id BIGSERIAL PRIMARY KEY,
    processo_id BIGINT NOT NULL,
    seq BIGINT NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload TEXT NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    actor_user_id BIGINT,
    actor_role VARCHAR(60),
    created_at TIMESTAMP NOT NULL
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_processo_event_processo'
    ) THEN
        ALTER TABLE tb_processo_event
            ADD CONSTRAINT fk_processo_event_processo FOREIGN KEY (processo_id)
                REFERENCES tb_processo(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_processo_event_actor'
    ) THEN
        ALTER TABLE tb_processo_event
            ADD CONSTRAINT fk_processo_event_actor FOREIGN KEY (actor_user_id)
                REFERENCES tb_usuario(id) ON DELETE SET NULL;
    END IF;
END $$;

-- seq monotônico e hash idempotente por processo
CREATE UNIQUE INDEX IF NOT EXISTS ux_processo_event_seq ON tb_processo_event(processo_id, seq);
CREATE UNIQUE INDEX IF NOT EXISTS ux_processo_event_hash ON tb_processo_event(processo_id, payload_hash);

CREATE INDEX IF NOT EXISTS idx_processo_event_type ON tb_processo_event(processo_id, event_type);
CREATE INDEX IF NOT EXISTS idx_processo_event_created ON tb_processo_event(processo_id, created_at);
