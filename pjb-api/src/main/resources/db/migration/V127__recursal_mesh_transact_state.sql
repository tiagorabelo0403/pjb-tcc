create table if not exists tb_recursal_mesh_aggregate (
    recurso_id varchar(160) primary key,
    processo_id bigint null,
    numero_processo varchar(50),
    species_code varchar(30) not null,
    species_name varchar(160) not null,
    profile_name varchar(120) not null,
    current_state varchar(40) not null,
    tribunal_atual varchar(20) not null,
    instancia_atual varchar(20) not null,
    autoridade_atual varchar(30) not null,
    preparo_satisfeito boolean not null default false,
    admissibilidade_positiva boolean not null default false,
    remetido boolean not null default false,
    autuado_destino boolean not null default false,
    distribuido_destino boolean not null default false,
    preparo_em_complementacao boolean not null default false,
    diligencia_pendente boolean not null default false,
    multa_embargos boolean not null default false,
    sobrestado_precedente boolean not null default false,
    iteracoes_embargos integer not null default 0,
    snapshot_json text not null,
    route_plan_json text not null,
    context_json text not null,
    row_version bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint fk_recursal_mesh_aggregate_processo foreign key (processo_id) references tb_processo(id)
);

create index if not exists idx_recursal_mesh_aggregate_processo on tb_recursal_mesh_aggregate(processo_id);
create index if not exists idx_recursal_mesh_aggregate_state on tb_recursal_mesh_aggregate(current_state, updated_at desc);

create table if not exists tb_recursal_mesh_transition_ledger (
    id bigserial primary key,
    recurso_id varchar(160) not null,
    processo_id bigint null,
    species_code varchar(30) not null,
    profile_name varchar(120) not null,
    event_code varchar(40) not null,
    from_state varchar(40) not null,
    to_state varchar(40) not null,
    from_revision integer not null,
    to_revision integer not null,
    actor varchar(160),
    occurred_at timestamptz not null,
    snapshot_json text not null,
    route_plan_json text not null,
    created_at timestamptz not null default now(),
    constraint fk_recursal_mesh_ledger_processo foreign key (processo_id) references tb_processo(id)
);

create index if not exists idx_recursal_mesh_ledger_recurso_revision on tb_recursal_mesh_transition_ledger(recurso_id, to_revision desc);
create index if not exists idx_recursal_mesh_ledger_occurred_at on tb_recursal_mesh_transition_ledger(occurred_at desc);
