alter table if exists tb_pjb_substituicao_execucao
    add column if not exists ver bigint not null default 0;

create table if not exists tb_pjb_subst_homologacao_probe (
    id bigserial primary key,
    ver bigint not null default 0,
    execucao_id bigint not null references tb_pjb_substituicao_execucao (id) on delete cascade,
    tribunal_codigo varchar(24) not null,
    probe_codigo varchar(80) not null,
    connector_codigo varchar(40) not null,
    ambiente_codigo varchar(24) not null,
    situacao varchar(24) not null,
    gate_score integer not null default 0,
    evidencias_json text not null,
    resultado_json text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_pjb_subst_hom_probe_exec_probe unique (execucao_id, probe_codigo)
);

create index if not exists ix_pjb_subst_hom_probe_exec on tb_pjb_subst_homologacao_probe (execucao_id, probe_codigo);
create index if not exists ix_pjb_subst_hom_probe_status on tb_pjb_subst_homologacao_probe (tribunal_codigo, situacao, updated_at desc);

create table if not exists tb_pjb_subst_migracao_lote (
    id bigserial primary key,
    ver bigint not null default 0,
    execucao_id bigint not null references tb_pjb_substituicao_execucao (id) on delete cascade,
    tribunal_codigo varchar(24) not null,
    lote_codigo varchar(64) not null,
    lote_ordem integer not null,
    faixa_referencia varchar(160) not null,
    total_itens integer not null default 0,
    situacao varchar(32) not null,
    checksum_esperado varchar(128) not null,
    checksum_apurado varchar(128) null,
    divergencias integer not null default 0,
    snapshot_json text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_pjb_subst_mig_lote_exec_codigo unique (execucao_id, lote_codigo)
);

create index if not exists ix_pjb_subst_mig_lote_exec on tb_pjb_subst_migracao_lote (execucao_id, lote_ordem);
create index if not exists ix_pjb_subst_mig_lote_status on tb_pjb_subst_migracao_lote (tribunal_codigo, situacao, updated_at desc);

create table if not exists tb_pjb_subst_com_sync_cursor (
    id bigserial primary key,
    ver bigint not null default 0,
    execucao_id bigint not null references tb_pjb_substituicao_execucao (id) on delete cascade,
    tribunal_codigo varchar(24) not null,
    canal_origem varchar(48) not null,
    janela_inicio timestamptz not null,
    janela_fim timestamptz not null,
    correlation_namespace varchar(120) not null,
    dedupe_namespace varchar(120) not null,
    situacao varchar(32) not null,
    total_recebido integer not null default 0,
    total_deduplicado integer not null default 0,
    total_correlacionado integer not null default 0,
    total_reprocessavel integer not null default 0,
    snapshot_json text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_pjb_subst_com_cursor_exec_window unique (execucao_id, canal_origem, janela_inicio, janela_fim)
);

create index if not exists ix_pjb_subst_com_cursor_exec on tb_pjb_subst_com_sync_cursor (execucao_id, janela_inicio);
create index if not exists ix_pjb_subst_com_cursor_status on tb_pjb_subst_com_sync_cursor (tribunal_codigo, canal_origem, situacao, updated_at desc);

create table if not exists tb_pjb_subst_com_sync_item (
    id bigserial primary key,
    ver bigint not null default 0,
    cursor_id bigint not null references tb_pjb_subst_com_sync_cursor (id) on delete cascade,
    dedupe_hash varchar(128) not null,
    external_message_id varchar(180) null,
    correlation_key varchar(180) not null,
    processo_numero varchar(64) null,
    situacao varchar(32) not null,
    reprocessavel boolean not null default false,
    payload_json text not null,
    resultado_json text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_pjb_subst_com_item_cursor_dedupe unique (cursor_id, dedupe_hash)
);

create index if not exists ix_pjb_subst_com_item_cursor on tb_pjb_subst_com_sync_item (cursor_id, created_at);
create index if not exists ix_pjb_subst_com_item_corr on tb_pjb_subst_com_sync_item (correlation_key, situacao);
