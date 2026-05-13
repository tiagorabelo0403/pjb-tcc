set search_path to public;

create table if not exists tb_judicial_connector_telemetry (
    id uuid primary key,
    processo_id bigint null,
    numero_unificado varchar(50) null,
    tribunal_codigo varchar(20) null,
    unidade_judiciaria_codigo varchar(80) null,
    connector_system varchar(40) null,
    event_type varchar(60) not null,
    status varchar(80) null,
    accepted boolean null,
    protocol_reference varchar(120) null,
    message varchar(1000) null,
    payload_json text null,
    created_at timestamptz not null default now()
);

create index if not exists idx_judicial_connector_telemetry_created_at on tb_judicial_connector_telemetry (created_at desc);
create index if not exists idx_judicial_connector_telemetry_system_created on tb_judicial_connector_telemetry (connector_system, created_at desc);
create index if not exists idx_judicial_connector_telemetry_numero on tb_judicial_connector_telemetry (numero_unificado);
create index if not exists idx_judicial_connector_telemetry_processo on tb_judicial_connector_telemetry (processo_id);
