-- Histórico de versões (snapshots) do conteúdo de um rascunho de peça inicial.
-- Cada autosave que altera o conteúdo grava uma versão, permitindo recuperar o rascunho apos
-- queda de energia/conexao e voltar a estados anteriores. O rascunho ativo continua em
-- tb_laiane_peticao_inicial_draft; aqui fica so o historico imutavel.
create table tb_peticao_draft_versao (
    id bigint generated always as identity primary key,
    draft_id bigint not null,
    versao_seq integer not null,
    origem varchar(20) not null,
    titulo_caso varchar(180),
    minuta_html text,
    fatos_json text,
    pedidos_json text,
    fundamentos_json text,
    provas_json text,
    hash_integridade varchar(64) not null,
    created_at timestamptz not null default now()
);

create index idx_peticao_draft_versao_draft on tb_peticao_draft_versao (draft_id, versao_seq);
