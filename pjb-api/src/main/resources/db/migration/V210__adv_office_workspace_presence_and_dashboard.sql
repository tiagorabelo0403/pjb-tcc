create table if not exists adv_office_workspace_presence (
    id bigserial primary key,
    equipe_id bigint not null,
    user_id bigint not null,
    membro_equipe_id bigint,
    office_mode varchar(20) not null,
    source_path varchar(255),
    last_seen_at timestamptz not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint fk_adv_office_presence_equipe foreign key (equipe_id) references equipes(id),
    constraint uk_adv_office_presence_equipe_user unique (equipe_id, user_id)
);

create index if not exists idx_adv_office_presence_equipe_last_seen
    on adv_office_workspace_presence (equipe_id, last_seen_at desc);

create index if not exists idx_adv_office_presence_user_last_seen
    on adv_office_workspace_presence (user_id, last_seen_at desc);
