create table if not exists tb_authz_trail_analytics (
    id bigserial primary key,
    granularity varchar(16) not null,
    bucket_started_at timestamp not null,
    bucket_ended_at_exclusive timestamp not null,
    dimension_type varchar(64) not null,
    dimension_code varchar(160) not null,
    dimension_label varchar(160) not null,
    total_count bigint not null,
    allowed_count bigint not null,
    denied_count bigint not null,
    critico_count bigint not null,
    governance_denied_count bigint not null,
    step_up_denied_count bigint not null,
    unique_request_count bigint not null,
    unique_actor_count bigint not null,
    first_occurred_at timestamp not null,
    last_occurred_at timestamp not null,
    materialized_at timestamp not null,
    source_event_count bigint not null,
    analytics_key varchar(280) not null unique
);

create index if not exists idx_authz_analytics_bucket
    on tb_authz_trail_analytics (granularity, bucket_started_at, dimension_type);

create index if not exists idx_authz_analytics_dimension
    on tb_authz_trail_analytics (dimension_type, dimension_code, bucket_started_at);

create index if not exists idx_authz_analytics_materialized
    on tb_authz_trail_analytics (materialized_at);
