create table if not exists tb_triagem_nacional_analise (
    id uuid primary key,
    nupn_provisorio varchar(80) not null,
    processo_id bigint null,
    documento_autor varchar(14) null,
    documento_reu varchar(14) null,
    veredito varchar(40) not null,
    confianca_geral numeric(8,4) null,
    aprovacao_automatica boolean not null default false,
    classe_tpu varchar(120) null,
    assunto_tpu varchar(180) null,
    prescricao_aparente boolean not null default false,
    decadencia_aparente boolean not null default false,
    competencia_sugerida varchar(40) null,
    rito_sugerido varchar(80) null,
    pendencias_json text null,
    conexos_json text null,
    resumo_decisao text null,
    request_hash varchar(64) not null,
    triado_em timestamp with time zone not null,
    atualizado_em timestamp with time zone not null
);

create index if not exists idx_triagem_nacional_nupn on tb_triagem_nacional_analise (nupn_provisorio);
create index if not exists idx_triagem_nacional_processo on tb_triagem_nacional_analise (processo_id);
create index if not exists idx_triagem_nacional_doc_autor on tb_triagem_nacional_analise (documento_autor);
create index if not exists idx_triagem_nacional_doc_reu on tb_triagem_nacional_analise (documento_reu);
create index if not exists idx_triagem_nacional_veredito on tb_triagem_nacional_analise (veredito);
