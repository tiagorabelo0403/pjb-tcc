create table if not exists tb_professional_access_grant_template (
    id bigserial primary key,
    template_code varchar(80) not null,
    label varchar(180) not null,
    description varchar(800),
    actor_class varchar(40) not null,
    grant_type varchar(40) not null,
    access_basis varchar(60) not null,
    default_requires_step_up boolean not null default false,
    auto_approve_allowed boolean not null default false,
    default_duration_days integer,
    target_mode varchar(40) not null,
    default_uf varchar(5),
    default_comarca varchar(160),
    default_tribunal varchar(80),
    default_unidade_judiciaria_codigo varchar(80),
    default_orgao_colegiado_codigo varchar(80),
    default_ente_code varchar(80),
    governance_tone varchar(40),
    sequence_order integer,
    ativo boolean not null default true
);

create unique index if not exists idx_prof_access_grant_template_code on tb_professional_access_grant_template (template_code);
create index if not exists idx_prof_access_grant_template_actor on tb_professional_access_grant_template (actor_class, grant_type, ativo);
create index if not exists idx_prof_access_grant_template_sequence on tb_professional_access_grant_template (sequence_order, id);

insert into tb_professional_access_grant_template (
    template_code,
    label,
    description,
    actor_class,
    grant_type,
    access_basis,
    default_requires_step_up,
    auto_approve_allowed,
    default_duration_days,
    target_mode,
    governance_tone,
    sequence_order,
    ativo
) values
('MAG_RELATORIA_EXPRESSA', 'Relatoria expressa por processo', 'Template para relatoria ativa vinculada a processo específico, com janela operacional curta e trilha jurisdicional forte.', 'MAGISTRATURA', 'RELATORIA_PROCESSO', 'MAGISTRATURA_RELATORIA_ATIVA', false, true, 90, 'PROCESS_AND_TARGET', 'ACTIVE_BLUE', 10, true),
('MAG_PLANTAO_CRITICO', 'Plantão jurisdicional crítico', 'Template para plantão ativo, com step-up e recorte territorial de urgência.', 'MAGISTRATURA', 'PLANTAO', 'MAGISTRATURA_PLANTAO_ATIVO', true, false, 7, 'TARGET_AND_TERRITORY', 'ATTENTION_ORANGE', 20, true),
('MAG_DELEGACAO_GABINETE', 'Delegação formal de gabinete', 'Template para apoio judicial em gabinete, com escopo controlado e governança reforçada.', 'APOIO_JUDICIAL', 'DELEGACAO_GABINETE', 'APOIO_JUDICIAL_DELEGACAO_FORMAL', true, false, 30, 'TARGET_AND_MAGISTRATE', 'ATTENTION_ORANGE', 30, true),
('DEF_PROCESSO_ASSISTIDO', 'Designação processual do assistido', 'Template para defensoria em processo específico com base formal de assistência.', 'DEFENSORIA', 'DESIGNACAO_PROCESSO', 'DEFENSORIA_DESIGNACAO_FORMAL', false, true, 180, 'PROCESS_AND_TARGET', 'ACTIVE_BLUE', 40, true),
('DEF_TERRITORIO_UNIDADE', 'Designação territorial da unidade', 'Template para janela territorial da defensoria vinculada a comarca, unidade ou tribunal.', 'DEFENSORIA', 'DESIGNACAO_TERRITORIAL', 'DEFENSORIA_ATUACAO_TERRITORIAL', true, false, 60, 'TARGET_AND_TERRITORY', 'ATTENTION_ORANGE', 50, true),
('PROC_ENTE_FORMAL', 'Representação formal do ente', 'Template para procuradoria por ente público representado, com governança territorial e institucional.', 'PROCURADORIA', 'REPRESENTACAO_ENTE', 'PROCURADORIA_REPRESENTACAO_FORMAL', false, true, 180, 'TARGET_AND_ENTE', 'ACTIVE_BLUE', 60, true),
('PROC_PROCESSO_FORMAL', 'Representação processual formal', 'Template para procuradoria em processo específico, com base institucional e trilha auditável.', 'PROCURADORIA', 'REPRESENTACAO_PROCESSO', 'PROCURADORIA_REPRESENTACAO_FORMAL', false, true, 120, 'PROCESS_AND_TARGET', 'ACTIVE_BLUE', 70, true);
