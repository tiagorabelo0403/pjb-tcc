create table if not exists tb_radar_padrao_analise (
    id bigserial primary key,
    uuid uuid not null unique,
    processo_id bigint null,
    nupn varchar(80) not null,
    tribunal_codigo varchar(30) null,
    documento_autor_hash varchar(64) null,
    documento_reu_hash varchar(64) null,
    escritorio_oab_hash varchar(64) null,
    fingerprint_estrutura_hash varchar(64) null,
    fingerprint_conteudo_hash varchar(64) null,
    numero_paragrafos integer null,
    total_palavras integer null,
    densidade_jargao numeric(10,6) null,
    diversidade_lexica numeric(10,6) null,
    valor_causa numeric(19,2) null,
    data_ajuizamento date null,
    score_geral numeric(10,6) null,
    nivel_mais_alto varchar(30) null,
    total_alertas integer null,
    tipos_detectados varchar(600) null,
    resumo_tecnico text null,
    request_json text null,
    response_json text null,
    gerado_em timestamp with time zone not null,
    constraint fk_radar_analise_processo foreign key (processo_id) references tb_processo(id)
);

create table if not exists tb_radar_padrao_alerta (
    id bigserial primary key,
    uuid uuid not null unique,
    analise_id bigint null,
    processo_id bigint null,
    nupn varchar(80) not null,
    tipo_padrao varchar(80) not null,
    nivel varchar(30) not null,
    score numeric(10,6) null,
    descricao_tecnica text null,
    evidencias_objetivas text null,
    orientacao_magistrado text null,
    processo_nao_bloqueado boolean not null default true,
    referencia_teto varchar(80) null,
    explicacao_financeira_ia text null,
    nupns_relacionados_json text null,
    chave_deteccao varchar(64) null unique,
    detectado_em timestamp with time zone not null,
    constraint fk_radar_alerta_analise foreign key (analise_id) references tb_radar_padrao_analise(id),
    constraint fk_radar_alerta_processo foreign key (processo_id) references tb_processo(id)
);

create index if not exists idx_radar_analise_processo on tb_radar_padrao_analise (processo_id, gerado_em desc);
create index if not exists idx_radar_analise_nupn on tb_radar_padrao_analise (nupn, gerado_em desc);
create index if not exists idx_radar_analise_escritorio on tb_radar_padrao_analise (escritorio_oab_hash, gerado_em desc);
create index if not exists idx_radar_analise_autor on tb_radar_padrao_analise (documento_autor_hash, gerado_em desc);
create index if not exists idx_radar_analise_reu on tb_radar_padrao_analise (documento_reu_hash, gerado_em desc);
create index if not exists idx_radar_analise_fp1 on tb_radar_padrao_analise (fingerprint_estrutura_hash, gerado_em desc);
create index if not exists idx_radar_analise_fp2 on tb_radar_padrao_analise (fingerprint_conteudo_hash, gerado_em desc);
create index if not exists idx_radar_alerta_processo on tb_radar_padrao_alerta (processo_id, detectado_em desc);
create index if not exists idx_radar_alerta_nupn on tb_radar_padrao_alerta (nupn, detectado_em desc);
create index if not exists idx_radar_alerta_tipo on tb_radar_padrao_alerta (tipo_padrao, nivel);
