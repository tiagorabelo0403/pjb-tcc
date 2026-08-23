-- JSON validado do TipTap como fonte de verdade do conteudo do rascunho de peca.
-- A minuta_inicial (HTML) passa a ser projecao derivada e segura, renderizada do JSON sanitizado;
-- conteudo_json guarda o documento autoritativo, tanto no rascunho ativo quanto no snapshot de versao.
alter table tb_laiane_peticao_inicial_draft
    add column if not exists conteudo_json text;

alter table tb_peticao_draft_versao
    add column if not exists conteudo_json text;
