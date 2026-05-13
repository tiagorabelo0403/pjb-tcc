create table if not exists tb_pjb_substituicao_execucao (
    id bigserial primary key,
    tribunal_codigo varchar(24) not null,
    tribunal_nome varchar(180) not null,
    ramo_justica varchar(48) not null,
    acao varchar(64) not null,
    situacao varchar(32) not null,
    fase_atual varchar(32) not null,
    modo_execucao varchar(24) not null,
    dry_run boolean not null default false,
    gate_aprovado boolean not null default false,
    rollback_reversivel boolean not null default false,
    gate_score integer not null default 0,
    job_id uuid null,
    correlation_id varchar(120) null,
    request_hash varchar(128) not null,
    requested_by varchar(120) not null,
    justificativa varchar(1000) null,
    onda_alvo varchar(64) null,
    payload_json text null,
    resultado_json text null,
    started_at timestamptz null,
    completed_at timestamptz null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_pjb_subst_exec_request_hash unique (request_hash)
);

create index if not exists ix_pjb_subst_exec_tribunal on tb_pjb_substituicao_execucao (tribunal_codigo, acao, situacao, created_at desc);
create index if not exists ix_pjb_subst_exec_job on tb_pjb_substituicao_execucao (job_id);

create table if not exists tb_pjb_substituicao_execucao_evento (
    id bigserial primary key,
    execucao_id bigint not null references tb_pjb_substituicao_execucao (id) on delete cascade,
    codigo varchar(80) not null,
    severidade varchar(16) not null,
    fase varchar(32) not null,
    descricao varchar(1000) not null,
    detalhes_json text null,
    created_at timestamptz not null default now()
);

create index if not exists ix_pjb_subst_exec_event_exec on tb_pjb_substituicao_execucao_evento (execucao_id, created_at asc);
