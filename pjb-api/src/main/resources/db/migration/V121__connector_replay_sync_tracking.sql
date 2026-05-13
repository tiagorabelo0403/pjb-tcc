alter table tb_processo
    add column if not exists connector_submission_attempts integer not null default 0,
    add column if not exists connector_last_submission_attempt_at timestamp,
    add column if not exists connector_sync_status varchar(80),
    add column if not exists connector_sync_message varchar(500),
    add column if not exists connector_snapshot_synced_at timestamp,
    add column if not exists connector_events_synced_at timestamp,
    add column if not exists connector_sync_attempts integer not null default 0;

create index if not exists idx_tb_processo_connector_submission_attempts on tb_processo (connector_submission_attempts);
create index if not exists idx_tb_processo_connector_sync_status on tb_processo (connector_sync_status);
create index if not exists idx_tb_processo_connector_last_attempt on tb_processo (connector_last_submission_attempt_at);
