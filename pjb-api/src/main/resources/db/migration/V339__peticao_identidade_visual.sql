-- Perfil de identidade visual (papel timbrado) persistido por ator peticionante.
-- O logo em si vai para object storage (logo_storage_key), nunca como blob no banco —
-- mesmo padrao de tb_usuario_avatar. escopo/escopo_ref ja previstos para estender a
-- identidade institucional (defensoria por estado, MP, procuradorias) sem alterar o schema.
create table tb_peticao_identidade_visual (
    id bigint generated always as identity primary key,
    usuario_id bigint not null,
    escopo varchar(20) not null default 'INDIVIDUAL',
    escopo_ref varchar(80),
    nome_exibicao varchar(600),
    nome_instituicao varchar(600),
    logo_storage_key text,
    logo_content_type varchar(80),
    logo_size_bytes bigint,
    logo_sha256 varchar(64),
    cabecalho_livre varchar(1500),
    rodape_livre varchar(1500),
    paleta_primaria varchar(16),
    paleta_secundaria varchar(16),
    exibir_registro_profissional boolean not null default true,
    exibir_brasao_logomarca boolean not null default true,
    ativo boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create unique index uk_peticao_identidade_usuario on tb_peticao_identidade_visual (usuario_id);
