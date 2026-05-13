alter table if exists tb_documento_processual
    add column if not exists visibility_scope varchar(80);

update tb_documento_processual
set visibility_scope = case
    when visibility_scope is not null and btrim(visibility_scope) <> '' then visibility_scope
    when categoria = 'PESSOAL' then 'COUNSEL_REPRESENTED_PARTY'
    when upper(coalesce(titulo, '') || ' ' || coalesce(nome_original, '')) like '%SENTEN%'
      or upper(coalesce(titulo, '') || ' ' || coalesce(nome_original, '')) like '%DECIS%'
      or upper(coalesce(titulo, '') || ' ' || coalesce(nome_original, '')) like '%DESPACH%'
      or upper(coalesce(titulo, '') || ' ' || coalesce(nome_original, '')) like '%ACÓRD%'
      or upper(coalesce(titulo, '') || ' ' || coalesce(nome_original, '')) like '%ACORDA%'
      or upper(coalesce(titulo, '') || ' ' || coalesce(nome_original, '')) like '%VOTO%'
      or upper(coalesce(titulo, '') || ' ' || coalesce(nome_original, '')) like '%PAUTA%'
      then 'PUBLIC_ACT'
    when coalesce(nivel_sigilo, 'PUBLICO') in ('SIGILO_N2', 'SIGILO_N3', 'SIGILO_N4', 'SEGREDO_JUSTICA', 'ABSOLUTO') then 'COUNSEL_REPRESENTED_PARTY'
    else 'PROFESSIONAL_NON_MANDATE_VIEW'
end
where visibility_scope is null or btrim(visibility_scope) = '';

create index if not exists idx_doc_processual_visibility_scope on tb_documento_processual (visibility_scope);
create index if not exists idx_doc_processual_proc_scope on tb_documento_processual (processo_id, visibility_scope);

create table if not exists tb_professional_process_view_audit (
    id bigserial primary key,
    usuario_id bigint not null,
    usuario_nome varchar(180) not null,
    oab_ou_matricula varchar(80),
    processo_id bigint not null,
    numero_processo varchar(80) not null,
    documento_id varchar(80),
    actor_class varchar(40) not null,
    panel_mode varchar(60) not null,
    access_basis varchar(80) not null,
    operation_type varchar(40) not null,
    query_type varchar(40),
    query_value_masked varchar(180),
    reason varchar(500),
    step_up_mode varchar(60),
    sucesso boolean not null default false,
    client_fingerprint_hash varchar(128),
    acessado_em timestamp not null default now()
);

create index if not exists idx_prof_view_audit_user_time on tb_professional_process_view_audit (usuario_id, acessado_em desc);
create index if not exists idx_prof_view_audit_proc_time on tb_professional_process_view_audit (processo_id, acessado_em desc);
create index if not exists idx_prof_view_audit_actor_time on tb_professional_process_view_audit (actor_class, acessado_em desc);
