create table if not exists tb_atlas_acesso_municipio (
    id bigserial primary key,
    uuid uuid not null unique,
    codigo_ibge varchar(7) not null unique,
    nome_municipio varchar(160) not null,
    uf varchar(2) not null,
    regiao varchar(30) not null,
    populacao integer not null,
    varas_instaladas integer not null,
    juizes_em_exercicio integer not null,
    defensorias_por_municipio integer not null,
    advogados_oab_ativos integer not null,
    tem_juizado_especial boolean not null,
    tem_cejusc boolean not null,
    processos_por_mil_habitantes integer not null,
    novos_processos_mes integer not null,
    taxa_resolutividade_pct numeric(10,4) not null,
    tempo_medio_resolucao_dias numeric(10,2) not null,
    indice_congestionamento numeric(10,4) not null,
    taxa_justica_gratuita_pct numeric(10,4) not null,
    taxa_auto_representacao_pct numeric(10,4) not null,
    taxa_prescricao_aparente_pct numeric(10,4) not null,
    score_infraestrutura numeric(10,4) not null,
    score_representacao numeric(10,4) not null,
    score_celeridade numeric(10,4) not null,
    score_efetividade numeric(10,4) not null,
    score_total numeric(10,4) not null,
    grau varchar(1) not null,
    classificacao varchar(30) not null,
    origem_dados varchar(60),
    atualizado_em timestamp with time zone not null
);

create index if not exists idx_atlas_municipio_codigo_ibge on tb_atlas_acesso_municipio(codigo_ibge);
create index if not exists idx_atlas_municipio_uf_class on tb_atlas_acesso_municipio(uf, classificacao);
create index if not exists idx_atlas_municipio_score on tb_atlas_acesso_municipio(score_total);
create index if not exists idx_atlas_municipio_pop on tb_atlas_acesso_municipio(populacao);
