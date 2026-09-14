-- Fase 2 da identidade visual: alem do perfil individual (usuario_id), passa a existir o perfil
-- INSTITUCIONAL do orgao (brasão/cores curados por admin do orgao), chaveado por escopo_ref
-- (ex.: PJ-EST-CE, MP-FED, DP-EST-SP, PROC-MUN-CE). usuario_id deixa de ser obrigatorio porque o
-- perfil institucional nao tem dono individual; uma unica linha institucional por escopo_ref.
alter table tb_peticao_identidade_visual
    alter column usuario_id drop not null;

create unique index uk_peticao_identidade_institucional
    on tb_peticao_identidade_visual (escopo_ref)
    where escopo = 'INSTITUCIONAL';
