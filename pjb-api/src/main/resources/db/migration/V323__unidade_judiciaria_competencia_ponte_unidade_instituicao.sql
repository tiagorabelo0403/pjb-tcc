alter table tb_unidade_judiciaria_competencia
    add column unidade_instituicao_id bigint references tb_unidade_institucional(id);

create index if not exists idx_unidade_competencia_unidade_instituicao
    on tb_unidade_judiciaria_competencia(unidade_instituicao_id);
