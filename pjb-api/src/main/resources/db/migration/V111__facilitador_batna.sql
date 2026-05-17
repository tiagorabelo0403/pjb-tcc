create table if not exists tb_batna_relatorio (
    id bigserial primary key,
    uuid uuid not null unique,
    processo_id bigint null,
    proposta_acordo_id bigint null,
    nupn varchar(80) not null,
    tribunal_codigo varchar(30) null,
    ramo_direito varchar(80) null,
    classe_tpu varchar(120) null,
    fase_processual varchar(80) null,
    valor_causa numeric(19,2) null,
    valor_pedido_principal numeric(19,2) null,
    valor_acordo_em_discussao numeric(19,2) null,
    custo_autor_total numeric(19,2) null,
    custo_reu_total numeric(19,2) null,
    custo_combinado numeric(19,2) null,
    dias_restantes_sem_recurso integer null,
    dias_restantes_com_recurso integer null,
    prob_recurso numeric(10,6) null,
    prob_reforma numeric(10,6) null,
    indice_pressao_economica numeric(10,6) null,
    confianca_geral numeric(10,6) null,
    teto_violacao boolean not null default false,
    teto_tipo varchar(80) null,
    teto_limite numeric(19,2) null,
    teto_excedente numeric(19,2) null,
    hash_contexto varchar(64) not null,
    resumo_narrativo text null,
    resumo_tecnico text null,
    request_json text null,
    response_json text null,
    gerado_em timestamp with time zone not null,
    constraint fk_batna_processo foreign key (processo_id) references tb_processo(id)
);

create index if not exists idx_batna_processo on tb_batna_relatorio (processo_id, gerado_em desc);
create index if not exists idx_batna_proposta on tb_batna_relatorio (proposta_acordo_id, gerado_em desc);
create index if not exists idx_batna_hash_contexto on tb_batna_relatorio (hash_contexto);
create index if not exists idx_batna_nupn on tb_batna_relatorio (nupn);
