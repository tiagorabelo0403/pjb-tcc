alter table if exists tb_professional_access_grant
    add column if not exists approval_status varchar(20) not null default 'PENDING',
    add column if not exists requested_by_user_id bigint,
    add column if not exists requested_by_name varchar(180),
    add column if not exists requested_at timestamp,
    add column if not exists approved_by_user_id bigint,
    add column if not exists approved_by_name varchar(180),
    add column if not exists approved_at timestamp,
    add column if not exists decision_reason varchar(800),
    add column if not exists revoked_by_user_id bigint,
    add column if not exists revoked_by_name varchar(180),
    add column if not exists revoked_at timestamp;

update tb_professional_access_grant
set approval_status = 'APPROVED'
where approval_status is null;

create index if not exists idx_prof_access_grant_status on tb_professional_access_grant(approval_status, actor_class, grant_type);
create index if not exists idx_prof_access_grant_requested_by on tb_professional_access_grant(requested_by_user_id, requested_at desc);
create index if not exists idx_prof_access_grant_target_magistrate on tb_professional_access_grant(target_magistrate_user_id, approval_status);

create table if not exists tb_professional_access_grant_event (
    id bigserial primary key,
    grant_id bigint not null,
    event_type varchar(20) not null,
    previous_status varchar(20),
    new_status varchar(20),
    actor_user_id bigint not null,
    actor_name varchar(180) not null,
    actor_class varchar(40) not null,
    detail varchar(1000),
    created_at timestamp not null default now(),
    constraint fk_prof_access_grant_event_grant
        foreign key (grant_id) references tb_professional_access_grant(id)
);

create index if not exists idx_prof_access_grant_event_grant_time on tb_professional_access_grant_event(grant_id, created_at desc);
create index if not exists idx_prof_access_grant_event_actor_time on tb_professional_access_grant_event(actor_user_id, created_at desc);
