alter table tb_recursal_mesh_aggregate
    add column if not exists tribunal_detalhado_atual varchar(20);

update tb_recursal_mesh_aggregate
   set tribunal_detalhado_atual = coalesce(tribunal_detalhado_atual,
       case tribunal_atual
           when 'TJ' then 'TJSP'
           when 'TRF' then 'TRF1'
           when 'TRT' then 'TRT1'
           when 'TRE' then 'TREDF'
           when 'STJ' then 'STJ'
           when 'TST' then 'TST'
           when 'STF' then 'STF'
           else 'TJSP'
       end);

alter table tb_recursal_mesh_aggregate
    alter column tribunal_detalhado_atual set not null;
