create table if not exists tb_diligencia_operador_checkpoint (
    id bigserial primary key,
    operator_user_id bigint not null,
    operator_tipo_usuario varchar(80) not null,
    canal varchar(40) not null,
    diligence_reference varchar(120) not null,
    checkpoint_tipo varchar(40) not null,
    target_latitude double precision not null,
    target_longitude double precision not null,
    observed_latitude double precision not null,
    observed_longitude double precision not null,
    distance_meters double precision not null,
    geofence_radius_meters double precision not null,
    inside_geofence boolean not null,
    classification varchar(40) not null,
    source varchar(40) not null,
    device_hash varchar(64),
    occurred_at timestamptz not null,
    created_at timestamptz not null,
    request_id varchar(80),
    ip varchar(80)
);

create index if not exists idx_diligencia_checkpoint_user_ref
    on tb_diligencia_operador_checkpoint (operator_user_id, canal, diligence_reference, occurred_at desc);

create index if not exists idx_diligencia_checkpoint_request
    on tb_diligencia_operador_checkpoint (request_id);

create index if not exists idx_diligencia_checkpoint_device
    on tb_diligencia_operador_checkpoint (device_hash, occurred_at desc);

create table if not exists tb_cadeia_custodia_digital_sync_event (
    id bigserial primary key,
    chave_custodia varchar(32) not null,
    digest_colecao_sha256 varchar(64) not null,
    direcao varchar(20) not null,
    operacao varchar(20) not null,
    parceiro_institucional varchar(120) not null,
    no_origem varchar(120) not null,
    request_nonce varchar(80) not null,
    payload_digest_sha256 varchar(64) not null,
    assinatura_hmac_sha256 varchar(64) not null,
    integridade_ok boolean not null,
    assinatura_ok boolean not null,
    correspondencia_local_ok boolean not null,
    total_entradas integer not null,
    actor_user_id bigint,
    actor_perfil varchar(80) not null,
    occurred_at timestamptz not null,
    created_at timestamptz not null,
    request_id varchar(80),
    ip varchar(80)
);

create index if not exists idx_custodia_sync_event_chave_data
    on tb_cadeia_custodia_digital_sync_event (chave_custodia, occurred_at desc);

create index if not exists idx_custodia_sync_event_nonce
    on tb_cadeia_custodia_digital_sync_event (request_nonce);

create index if not exists idx_custodia_sync_event_payload
    on tb_cadeia_custodia_digital_sync_event (payload_digest_sha256);

create index if not exists idx_custodia_sync_event_request
    on tb_cadeia_custodia_digital_sync_event (request_id);
