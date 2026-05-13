CREATE TABLE IF NOT EXISTS tb_database_retention_policy (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(120) NOT NULL UNIQUE,
    retention_window_days INTEGER NOT NULL,
    archive_strategy VARCHAR(60) NOT NULL,
    purge_mode VARCHAR(40) NOT NULL,
    legal_hold_supported BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_database_retention_policy_window ON tb_database_retention_policy (retention_window_days);

INSERT INTO tb_partition_plan (table_name, partition_column, partition_prefix, start_year, years_ahead, status, notes)
VALUES
    ('tb_work_item', 'created_at', 'tb_work_item_y', 2024, 3, 'ATIVO', 'Fila operacional quente com vocacao para particao anual e pruning administrativo.'),
    ('tb_outbox_event', 'created_at', 'tb_outbox_event_y', 2024, 3, 'ATIVO', 'Outbox transacional com alto volume e materializacao por janela.'),
    ('tb_processo_event', 'created_at', 'tb_processo_event_y', 2024, 3, 'ATIVO', 'Event store processual para trilha auditavel e recomposicao.'),
    ('tb_ui_state_history', 'occurred_at', 'tb_ui_state_history_y', 2024, 3, 'ATIVO', 'Historico de interface e gavetas operacionais com retenção governada.'),
    ('notification_history', 'enviado_em', 'notification_history_y', 2024, 3, 'ATIVO', 'Historico de notificacoes multicanal e rastreio de ciencia.')
ON CONFLICT (table_name)
DO UPDATE SET
    partition_column = EXCLUDED.partition_column,
    partition_prefix = EXCLUDED.partition_prefix,
    start_year = EXCLUDED.start_year,
    years_ahead = EXCLUDED.years_ahead,
    status = EXCLUDED.status,
    notes = EXCLUDED.notes,
    updated_at = NOW();

INSERT INTO tb_database_retention_policy (table_name, retention_window_days, archive_strategy, purge_mode, legal_hold_supported, notes)
VALUES
    ('tb_outbox_event', 30, 'QUEUE_REPLAY_EXPORT', 'PURGE_AFTER_REPLAY', TRUE, 'Outbox quente com retenção curta e replay exportavel.'),
    ('tb_processo_event', 3650, 'KEEP_PRIMARY_AUDIT', 'NO_PURGE', TRUE, 'Trilha auditavel processual de retenção longa.'),
    ('tb_ui_state_history', 365, 'COLD_STORAGE_EXPORT', 'SOFT_DELETE_FIRST', TRUE, 'Historico operacional com exportacao fria e purge governado.'),
    ('notification_history', 730, 'COLD_STORAGE_EXPORT', 'PURGE_AFTER_RETENTION', TRUE, 'Rastreio de notificacao com janela governada e suporte a legal hold.'),
    ('tb_work_item', 1095, 'STATEFUL_ARCHIVE', 'PURGE_AFTER_ARCHIVE', TRUE, 'Fila operacional com arquivamento por estado e retenção estendida.')
ON CONFLICT (table_name)
DO UPDATE SET
    retention_window_days = EXCLUDED.retention_window_days,
    archive_strategy = EXCLUDED.archive_strategy,
    purge_mode = EXCLUDED.purge_mode,
    legal_hold_supported = EXCLUDED.legal_hold_supported,
    notes = EXCLUDED.notes,
    updated_at = NOW();
