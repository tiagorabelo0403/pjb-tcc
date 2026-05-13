create table if not exists tb_painel_tribunal_metrica (
    id bigserial primary key,
    codigo_tribunal varchar(20) not null,
    tribunal_nome varchar(180) not null,
    uf varchar(2) null,
    processos_ativos bigint not null default 0,
    ajuizados_hoje bigint not null default 0,
    ajuizados_semana bigint not null default 0,
    ajuizados_mes bigint not null default 0,
    sentenciados_mes bigint not null default 0,
    arquivados_mes bigint not null default 0,
    acordos_mes bigint not null default 0,
    indice_congestionamento numeric(10,6) not null default 0,
    tempo_medio_resolucao_dias numeric(12,2) not null default 0,
    taxa_conciliacao_pct numeric(12,4) not null default 0,
    processos_com_prazo_excedido bigint not null default 0,
    classificacao_desempenho varchar(20) not null,
    ultima_ocorrencia_em timestamp with time zone null,
    atualizado_em timestamp with time zone not null,
    constraint uk_painel_tribunal_codigo unique (codigo_tribunal)
);

create table if not exists tb_painel_materia_metrica (
    id bigserial primary key,
    chave_metrica varchar(240) not null,
    ramo_direito varchar(80) not null,
    assunto_tpu varchar(180) not null,
    total_processos bigint not null default 0,
    tempo_medio_resolucao_dias numeric(12,2) not null default 0,
    taxa_prescricao_pct numeric(12,4) not null default 0,
    taxa_conciliacao_pct numeric(12,4) not null default 0,
    tribunal_mais_ativo varchar(20) null,
    distribuicao_tribunais_json text null,
    atualizado_em timestamp with time zone not null,
    constraint uk_painel_materia_chave unique (chave_metrica)
);

create table if not exists tb_painel_alerta_prazo (
    id uuid primary key,
    nupn varchar(80) not null,
    tribunal_codigo varchar(20) not null,
    ramo_direito varchar(80) not null,
    dias_em_andamento bigint not null default 0,
    prazo_razoavel_dias bigint not null default 0,
    dias_excedidos bigint not null default 0,
    fase varchar(80) null,
    nivel varchar(20) not null,
    ativo boolean not null default true,
    criado_em timestamp with time zone not null,
    atualizado_em timestamp with time zone not null
);

create table if not exists tb_painel_serie_temporal_diaria (
    id bigserial primary key,
    chave_serie varchar(220) not null,
    data_referencia date not null,
    tribunal_codigo varchar(20) null,
    ramo_direito varchar(80) null,
    ajuizados bigint not null default 0,
    sentenciados bigint not null default 0,
    arquivados bigint not null default 0,
    acordos bigint not null default 0,
    tempo_medio_novos numeric(12,2) not null default 0,
    atualizado_em timestamp with time zone not null,
    constraint uk_painel_serie_chave unique (chave_serie)
);

create index if not exists idx_painel_tribunal_congestionamento on tb_painel_tribunal_metrica (indice_congestionamento);
create index if not exists idx_painel_tribunal_classificacao on tb_painel_tribunal_metrica (classificacao_desempenho);
create index if not exists idx_painel_tribunal_uf on tb_painel_tribunal_metrica (uf);
create index if not exists idx_painel_materia_total on tb_painel_materia_metrica (total_processos);
create index if not exists idx_painel_materia_ramo on tb_painel_materia_metrica (ramo_direito);
create index if not exists idx_painel_materia_tribunal on tb_painel_materia_metrica (tribunal_mais_ativo);
create index if not exists idx_painel_alerta_nupn on tb_painel_alerta_prazo (nupn);
create index if not exists idx_painel_alerta_tribunal on tb_painel_alerta_prazo (tribunal_codigo, ativo);
create index if not exists idx_painel_alerta_nivel on tb_painel_alerta_prazo (nivel, ativo);
create index if not exists idx_painel_serie_data on tb_painel_serie_temporal_diaria (data_referencia);
create index if not exists idx_painel_serie_tribunal on tb_painel_serie_temporal_diaria (tribunal_codigo);
create index if not exists idx_painel_serie_ramo on tb_painel_serie_temporal_diaria (ramo_direito);
