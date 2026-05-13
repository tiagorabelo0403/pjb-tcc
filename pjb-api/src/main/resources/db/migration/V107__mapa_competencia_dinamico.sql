create table if not exists tb_unidade_judiciaria_competencia (
    id bigserial primary key,
    codigo varchar(80) not null,
    tribunal_codigo varchar(20) not null,
    comarca varchar(120),
    uf varchar(2),
    tipo_justica varchar(30),
    ramo_direito varchar(60),
    tipo_vara varchar(40) not null,
    processos_ativos integer not null default 0,
    capacidade_maxima integer not null default 1,
    capacidade_reserva_percentual integer not null default 0,
    indice_congestionamento numeric(6,4) not null default 0,
    aceita_distribuicao boolean not null default true,
    permite_juizado_especial boolean not null default false,
    permite_urgencia boolean not null default false,
    permite_distribuicao_automatica boolean not null default true,
    somente_digital boolean not null default false,
    status_operacional varchar(20) not null default 'ATIVA',
    modo_operacao varchar(15) not null default 'HIBRIDA',
    endereco_fisico varchar(240),
    endereco_digital varchar(240),
    valor_causa_minimo numeric(19,2),
    valor_causa_maximo numeric(19,2),
    prioridade_estrategica integer not null default 0,
    distribuicoes_ultimas_24h integer not null default 0,
    ultima_distribuicao_em timestamp with time zone,
    criado_em timestamp with time zone not null default now(),
    atualizado_em timestamp with time zone not null default now(),
    versao bigint,
    constraint uk_unidade_judiciaria_competencia_codigo unique (codigo),
    constraint ck_unidade_competencia_reserva check (capacidade_reserva_percentual between 0 and 95),
    constraint ck_unidade_competencia_congestionamento check (indice_congestionamento between 0 and 1),
    constraint ck_unidade_competencia_capacidade check (capacidade_maxima > 0),
    constraint ck_unidade_competencia_prioridade check (prioridade_estrategica between 0 and 100)
);

create index if not exists idx_unidade_competencia_tribunal on tb_unidade_judiciaria_competencia (tribunal_codigo);
create index if not exists idx_unidade_competencia_territorio on tb_unidade_judiciaria_competencia (uf, comarca);
create index if not exists idx_unidade_competencia_status on tb_unidade_judiciaria_competencia (aceita_distribuicao, status_operacional);
create index if not exists idx_unidade_competencia_justica on tb_unidade_judiciaria_competencia (tipo_justica, ramo_direito, tipo_vara);

create table if not exists tb_unidade_judiciaria_especialidade (
    unidade_id bigint not null,
    especialidade varchar(120) not null,
    primary key (unidade_id, especialidade),
    constraint fk_unidade_competencia_especialidade foreign key (unidade_id) references tb_unidade_judiciaria_competencia (id) on delete cascade
);

create table if not exists tb_unidade_judiciaria_classe_tpu (
    unidade_id bigint not null,
    classe_tpu varchar(80) not null,
    primary key (unidade_id, classe_tpu),
    constraint fk_unidade_competencia_classe foreign key (unidade_id) references tb_unidade_judiciaria_competencia (id) on delete cascade
);

create table if not exists tb_unidade_judiciaria_assunto_tpu (
    unidade_id bigint not null,
    assunto_tpu varchar(80) not null,
    primary key (unidade_id, assunto_tpu),
    constraint fk_unidade_competencia_assunto foreign key (unidade_id) references tb_unidade_judiciaria_competencia (id) on delete cascade
);

create table if not exists tb_processo_distribuicao_competencia (
    id bigserial primary key,
    processo_id bigint,
    unidade_id bigint not null,
    nupn varchar(50),
    score_final double precision not null,
    score_territorial double precision not null,
    score_especialidade double precision not null,
    score_disponibilidade double precision not null,
    score_equilibrio double precision not null,
    score_aderencia_normativa double precision not null,
    distribuicao_automatica boolean not null default false,
    status varchar(20) not null,
    motivacao text,
    alertas text,
    fatores_revisao text,
    request_hash varchar(64),
    criado_em timestamp with time zone not null default now(),
    atualizado_em timestamp with time zone not null default now(),
    constraint fk_processo_dist_comp_processo foreign key (processo_id) references tb_processo (id) on delete set null,
    constraint fk_processo_dist_comp_unidade foreign key (unidade_id) references tb_unidade_judiciaria_competencia (id) on delete restrict
);

create index if not exists idx_proc_dist_comp_processo on tb_processo_distribuicao_competencia (processo_id);
create index if not exists idx_proc_dist_comp_unidade on tb_processo_distribuicao_competencia (unidade_id);
create index if not exists idx_proc_dist_comp_status on tb_processo_distribuicao_competencia (status);
create index if not exists idx_proc_dist_comp_nupn on tb_processo_distribuicao_competencia (nupn);
create index if not exists idx_proc_dist_comp_request_hash on tb_processo_distribuicao_competencia (request_hash);
