create table if not exists tb_julgamento_coverage_audit (
    id bigserial primary key,
    processo_id bigint not null,
    usuario_id bigint not null,
    act_type varchar(40) not null,
    overall_status varchar(20) not null,
    overall_score integer not null,
    ramo_snapshot varchar(60),
    rito_snapshot varchar(80),
    justica_snapshot varchar(40),
    classe_snapshot varchar(180),
    recursal_species varchar(20),
    highlights_json text,
    alertas_json text,
    bloqueios_json text,
    metadata_hash varchar(64),
    created_at timestamp null,
    constraint fk_julg_cov_audit_processo foreign key (processo_id) references tb_processo (id),
    constraint fk_julg_cov_audit_usuario foreign key (usuario_id) references tb_usuario (id)
);

create index if not exists idx_julg_cov_proc_created on tb_julgamento_coverage_audit (processo_id, created_at);
create index if not exists idx_julg_cov_user_created on tb_julgamento_coverage_audit (usuario_id, created_at);
create index if not exists idx_julg_cov_status_act on tb_julgamento_coverage_audit (overall_status, act_type);
