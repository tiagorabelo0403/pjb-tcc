create table if not exists tb_professional_access_grant (
    id bigserial primary key,
    usuario_id bigint not null,
    processo_id bigint null,
    actor_class varchar(40) not null,
    grant_type varchar(40) not null,
    access_basis varchar(60) not null,
    uf varchar(5) null,
    comarca varchar(160) null,
    tribunal varchar(80) null,
    unidade_judiciaria_codigo varchar(80) null,
    orgao_colegiado_codigo varchar(80) null,
    ente_code varchar(80) null,
    target_magistrate_user_id bigint null,
    source_ref varchar(120) null,
    source_label varchar(240) null,
    reason varchar(800) null,
    requires_step_up boolean not null default false,
    ativo boolean not null default true,
    inicio_vigencia timestamp null,
    fim_vigencia timestamp null,
    constraint fk_prof_access_grant_usuario foreign key (usuario_id) references tb_usuario (id),
    constraint fk_prof_access_grant_processo foreign key (processo_id) references tb_processo (id)
);

create index if not exists idx_prof_access_grant_user_active
    on tb_professional_access_grant (usuario_id, ativo, inicio_vigencia, fim_vigencia);

create index if not exists idx_prof_access_grant_actor_type
    on tb_professional_access_grant (actor_class, grant_type);

create index if not exists idx_prof_access_grant_processo
    on tb_professional_access_grant (processo_id);

create index if not exists idx_prof_access_grant_territory
    on tb_professional_access_grant (uf, comarca, tribunal, unidade_judiciaria_codigo);
