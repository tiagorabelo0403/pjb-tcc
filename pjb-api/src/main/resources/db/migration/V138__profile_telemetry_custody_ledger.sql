create table if not exists tb_diligencia_operador_telemetria (
    id bigserial primary key,
    operator_user_id bigint not null,
    operator_tipo_usuario varchar(80) not null,
    canal varchar(40) not null,
    device_hash varchar(64),
    latitude double precision not null,
    longitude double precision not null,
    precisao_metros double precision,
    velocidade_kmh double precision,
    bateria_percentual integer,
    fonte varchar(32) not null,
    foreground boolean not null default false,
    capturado_em timestamptz not null,
    created_at timestamptz not null,
    request_id varchar(80),
    ip varchar(80)
);

create index if not exists idx_diligencia_telemetria_user_channel_created
    on tb_diligencia_operador_telemetria (operator_user_id, canal, created_at desc);
create index if not exists idx_diligencia_telemetria_device_created
    on tb_diligencia_operador_telemetria (device_hash, created_at desc);
create index if not exists idx_diligencia_telemetria_request
    on tb_diligencia_operador_telemetria (request_id);

create table if not exists tb_cadeia_custodia_digital (
    id bigserial primary key,
    digest_colecao_sha256 varchar(64) not null,
    chave_custodia varchar(32) not null,
    lote_referencia varchar(160),
    ordem_lote integer not null,
    evidence_id varchar(120) not null,
    evidence_nome varchar(255) not null,
    digest_sha256 varchar(64) not null,
    evidence_chave_custodia varchar(32) not null,
    metadata_digest_sha256 varchar(64) not null,
    metadata_canonica text,
    sealed_by_user_id bigint,
    sealed_by_perfil varchar(80) not null,
    sealed_at timestamptz not null,
    request_id varchar(80),
    ip varchar(80),
    user_agent varchar(255),
    prev_hash varchar(64),
    entry_hash varchar(64) not null
);

create unique index if not exists uk_cadeia_custodia_lote_item
    on tb_cadeia_custodia_digital (digest_colecao_sha256, evidence_id, digest_sha256);
create unique index if not exists uk_cadeia_custodia_entry_hash
    on tb_cadeia_custodia_digital (entry_hash);
create index if not exists idx_cadeia_custodia_chave_data
    on tb_cadeia_custodia_digital (chave_custodia, sealed_at desc);
create index if not exists idx_cadeia_custodia_digest_data
    on tb_cadeia_custodia_digital (digest_colecao_sha256, sealed_at desc);
create index if not exists idx_cadeia_custodia_actor_data
    on tb_cadeia_custodia_digital (sealed_by_user_id, sealed_at desc);
create index if not exists idx_cadeia_custodia_request
    on tb_cadeia_custodia_digital (request_id);
