alter table tb_unidade_judiciaria_competencia
    add column if not exists grau varchar(20) not null default 'PRIMEIRO_GRAU',
    add column if not exists tipo_turma_recursal varchar(20);

alter table tb_unidade_judiciaria_competencia
    add constraint ck_unidade_competencia_turma_recursal_exige_segundo_grau
        check (tipo_turma_recursal is null or grau = 'SEGUNDO_GRAU');

create index if not exists idx_unidade_competencia_grau on tb_unidade_judiciaria_competencia (grau);
