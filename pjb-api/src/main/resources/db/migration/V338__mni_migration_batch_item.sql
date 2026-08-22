create table mni_migration_batch_item (
    id bigint generated always as identity primary key,
    tribunal_origem varchar(40),
    motivo varchar(80),
    xml text not null,
    status varchar(20) not null default 'PENDENTE',
    processo_id_local bigint,
    erro text,
    criado_em timestamptz not null default now(),
    processado_em timestamptz
);

create index ix_mni_migration_status_id on mni_migration_batch_item (status, id);
