alter table tb_processo
    add column if not exists uf_autor      varchar(2),
    add column if not exists comarca_autor varchar(120),
    add column if not exists cidade_autor  varchar(120),
    add column if not exists uf_reu        varchar(2),
    add column if not exists comarca_reu   varchar(120),
    add column if not exists cidade_reu    varchar(120);
