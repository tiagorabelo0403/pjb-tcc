create table if not exists tb_usuario_accessibility_pref (
  id bigserial primary key,
  usuario_id bigint not null,
  preset varchar(60) not null,
  suppress_suggestions boolean not null default false,
  source varchar(30) not null,
  accepted_at timestamp null,
  updated_at timestamp not null default now(),
  last_evaluated_at timestamp null,
  next_eligible_at timestamp null,
  last_suggestion_hash varchar(64) null,
  constraint uk_usuario_accessibility_pref unique (usuario_id),
  constraint fk_usuario_accessibility_pref_usuario
    foreign key (usuario_id) references tb_usuario(id)
);

create index if not exists idx_usuario_accessibility_pref_usuario
  on tb_usuario_accessibility_pref(usuario_id);

create table if not exists tb_accessibility_usage_snapshot (
  id bigserial primary key,
  usuario_id bigint not null,
  observed_at timestamp not null,
  score integer not null,
  confidence numeric(6,4) not null,
  top_reason_codes text not null,
  top_reasons text not null,
  metrics_json text not null,
  policy_version integer not null,
  suggestion_hash varchar(64) not null,
  constraint fk_accessibility_usage_snapshot_usuario
    foreign key (usuario_id) references tb_usuario(id)
);

create index if not exists idx_accessibility_usage_snapshot_usuario_time
  on tb_accessibility_usage_snapshot(usuario_id, observed_at desc);
