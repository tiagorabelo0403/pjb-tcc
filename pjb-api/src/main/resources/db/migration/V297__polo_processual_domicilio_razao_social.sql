alter table tb_polo_processual
    add column if not exists uf_domicilio        varchar(2),
    add column if not exists comarca_domicilio   varchar(120),
    add column if not exists municipio_domicilio varchar(120),
    add column if not exists razao_social        varchar(300);
