create table if not exists tb_inst_affiliation_snapshot (
    id bigserial primary key,
    ver bigint not null default 0,
    affiliation_id varchar(160) not null,
    destinatario_kind varchar(100) not null,
    unidade_codigo varchar(180) not null,
    orgao_sigla varchar(120) not null,
    status_codigo varchar(80) not null,
    hash_integridade varchar(128) not null,
    snapshot_json text not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint uk_inst_affiliation_id unique (affiliation_id)
);

create index if not exists idx_inst_affiliation_kind on tb_inst_affiliation_snapshot(destinatario_kind, updated_at);
create index if not exists idx_inst_affiliation_unidade on tb_inst_affiliation_snapshot(unidade_codigo, updated_at);
create index if not exists idx_inst_affiliation_status on tb_inst_affiliation_snapshot(status_codigo, updated_at);

create table if not exists tb_inst_nomination_snapshot (
    id bigserial primary key,
    ver bigint not null default 0,
    nomination_id varchar(160) not null,
    affiliation_id varchar(160) not null,
    nominated_user_id bigint not null,
    unidade_codigo varchar(180) not null,
    caixa_codigo varchar(180) not null,
    status_codigo varchar(80) not null,
    hash_integridade varchar(128) not null,
    snapshot_json text not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint uk_inst_nomination_id unique (nomination_id)
);

create index if not exists idx_inst_nomination_user on tb_inst_nomination_snapshot(nominated_user_id, updated_at);
create index if not exists idx_inst_nomination_unidade on tb_inst_nomination_snapshot(unidade_codigo, updated_at);
create index if not exists idx_inst_nomination_status on tb_inst_nomination_snapshot(status_codigo, updated_at);
