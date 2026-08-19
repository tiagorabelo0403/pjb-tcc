create index if not exists idx_polo_processual_oab
    on tb_polo_processual (oab_numero, oab_uf)
    where ativo = true and oab_numero is not null;
