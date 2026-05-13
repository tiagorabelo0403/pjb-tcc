create table if not exists tb_authz_trail_analytics_refresh_queue (
    id bigserial primary key,
    granularity varchar(16) not null,
    bucket_started_at timestamp not null,
    bucket_ended_at_exclusive timestamp not null,
    status varchar(24) not null,
    attempt_count integer not null default 0,
    requeue_requested boolean not null default false,
    next_visible_at timestamp not null,
    locked_at timestamp null,
    last_enqueued_at timestamp not null,
    last_processed_at timestamp null,
    last_error text not null default '',
    dedup_key varchar(160) not null unique
);

create index if not exists idx_authz_analytics_refresh_status
    on tb_authz_trail_analytics_refresh_queue (status, next_visible_at, bucket_started_at);

create index if not exists idx_authz_analytics_refresh_bucket
    on tb_authz_trail_analytics_refresh_queue (granularity, bucket_started_at);

create index if not exists idx_authz_analytics_refresh_processed
    on tb_authz_trail_analytics_refresh_queue (last_processed_at);

create index if not exists brin_authz_trail_occurred
    on tb_authz_trail_read_model using brin (occurred_at);

create index if not exists brin_authz_analytics_bucket_materialized
    on tb_authz_trail_analytics using brin (bucket_started_at, materialized_at);

insert into tb_database_retention_policy (table_name, retention_window_days, archive_strategy, purge_mode, legal_hold_supported, notes)
values ('tb_authz_trail_analytics_refresh_queue', 365, 'KEEP_RUNTIME_QUEUE', 'NO_PURGE', false, 'Fila persistente de refresh incremental dos buckets analiticos AUTHZ.')
on conflict (table_name)
do update set
    retention_window_days = excluded.retention_window_days,
    archive_strategy = excluded.archive_strategy,
    purge_mode = excluded.purge_mode,
    legal_hold_supported = excluded.legal_hold_supported,
    notes = excluded.notes,
    updated_at = now();
