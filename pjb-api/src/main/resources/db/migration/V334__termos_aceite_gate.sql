alter table passkey_sessions
    add column if not exists termos_pendentes boolean not null default false;

create table if not exists tb_termos_aceite (
    id bigserial primary key,
    usuario_id bigint not null,
    versao varchar(40) not null,
    aceito_em timestamp with time zone not null default now(),
    ip varchar(64),
    constraint fk_termos_aceite_usuario foreign key (usuario_id) references tb_usuario (id)
);

create index if not exists idx_termos_aceite_usuario on tb_termos_aceite (usuario_id, aceito_em desc);
