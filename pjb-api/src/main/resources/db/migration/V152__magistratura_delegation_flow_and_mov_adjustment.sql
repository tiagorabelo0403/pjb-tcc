create table if not exists tb_judge_delegation_flow (
    id bigserial primary key,
    request_uuid uuid not null unique,
    magistrate_user_id bigint not null,
    delegate_user_id bigint not null,
    scope varchar(40) not null,
    status varchar(20) not null,
    requested_reason varchar(600),
    device_binding_hash varchar(128),
    duration_minutes integer not null,
    requested_at timestamp not null default now(),
    approved_at timestamp,
    rejected_at timestamp,
    revoked_at timestamp,
    expires_at timestamp,
    token_jti varchar(80),
    approved_by_user_id bigint,
    rejected_by_user_id bigint,
    revoked_by_user_id bigint,
    constraint fk_jdf_magistrate foreign key (magistrate_user_id) references tb_usuario(id),
    constraint fk_jdf_delegate foreign key (delegate_user_id) references tb_usuario(id),
    constraint fk_jdf_approved_by foreign key (approved_by_user_id) references tb_usuario(id),
    constraint fk_jdf_rejected_by foreign key (rejected_by_user_id) references tb_usuario(id),
    constraint fk_jdf_revoked_by foreign key (revoked_by_user_id) references tb_usuario(id)
);
create index if not exists idx_jdf_mag_status on tb_judge_delegation_flow(magistrate_user_id, status, expires_at);
create index if not exists idx_jdf_delegate_status on tb_judge_delegation_flow(delegate_user_id, status, expires_at);
create index if not exists idx_jdf_jti on tb_judge_delegation_flow(token_jti);

create table if not exists tb_movimentacao_adjustment_audit (
    id bigserial primary key,
    request_uuid uuid not null unique,
    processo_id bigint not null,
    movimentacao_id bigint not null,
    requested_by_user_id bigint not null,
    mode varchar(32) not null,
    status varchar(24) not null,
    motivo varchar(800) not null,
    descricao_substitutiva text,
    original_hash varchar(64) not null,
    audit_hash varchar(64) not null,
    compliance_score integer not null,
    compliance_flags varchar(1200),
    compliance_verdict varchar(32) not null,
    created_at timestamp not null default now(),
    applied_at timestamp,
    ledger_entry_hash varchar(64),
    generated_movimentacao_id bigint,
    constraint fk_maa_processo foreign key (processo_id) references tb_processo(id),
    constraint fk_maa_movimentacao foreign key (movimentacao_id) references tb_movimentacao_processual(id),
    constraint fk_maa_requested_by foreign key (requested_by_user_id) references tb_usuario(id),
    constraint fk_maa_generated_mov foreign key (generated_movimentacao_id) references tb_movimentacao_processual(id)
);
create index if not exists idx_maa_processo_created on tb_movimentacao_adjustment_audit(processo_id, created_at desc);
create index if not exists idx_maa_mov_status on tb_movimentacao_adjustment_audit(movimentacao_id, status);
