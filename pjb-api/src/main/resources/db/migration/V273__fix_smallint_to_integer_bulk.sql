alter table pjb_ciencia_processual
    alter column prazo_resposta_dias type integer;

alter table tb_atendimento_thread_member_settings
    alter column quiet_hours_end_min   type integer,
    alter column quiet_hours_start_min type integer;

alter table tb_carga_processo
    alter column volumes type integer;

alter table tb_pauta_stf
    alter column ministros_impedidos type integer,
    alter column votos_contrarios    type integer,
    alter column votos_favoraveis    type integer,
    alter column votos_vista         type integer;

alter table tb_polo_processual
    alter column ordem_polo type integer;
