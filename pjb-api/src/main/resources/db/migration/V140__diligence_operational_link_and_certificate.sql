alter table if exists tb_diligencia_operador_checkpoint
    add column if not exists work_item_id bigint,
    add column if not exists processo_id bigint,
    add column if not exists processo_numero varchar(32),
    add column if not exists work_item_template_code varchar(120),
    add column if not exists work_item_type varchar(40),
    add column if not exists work_item_status varchar(30),
    add column if not exists tentativa_sequencia integer not null default 1,
    add column if not exists location_signature_sha256 varchar(64);

create index if not exists idx_diligencia_checkpoint_workitem
    on tb_diligencia_operador_checkpoint (work_item_id, occurred_at desc);

create index if not exists idx_diligencia_checkpoint_processo
    on tb_diligencia_operador_checkpoint (processo_id, occurred_at desc);

create table if not exists tb_diligencia_operador_certidao (
    id bigserial primary key,
    operator_user_id bigint not null,
    operator_tipo_usuario varchar(80) not null,
    canal varchar(40) not null,
    diligence_reference varchar(120) not null,
    work_item_id bigint,
    processo_id bigint,
    processo_numero varchar(32),
    checkpoint_event_id bigint,
    certidao_tipo varchar(40) not null,
    titulo varchar(180) not null,
    narrativa text not null,
    certificate_digest_sha256 varchar(64) not null,
    signature_hmac_sha256 varchar(64) not null,
    latitude double precision,
    longitude double precision,
    destino_latitude double precision,
    destino_longitude double precision,
    distance_meters double precision,
    inside_geofence boolean,
    tentativa_sequencia integer,
    evidence_chave_custodia varchar(32),
    attempt_trail_digest_sha256 varchar(64),
    request_id varchar(80),
    created_at timestamptz not null default now()
);

create index if not exists idx_diligencia_certidao_user_ref
    on tb_diligencia_operador_certidao (operator_user_id, canal, diligence_reference, created_at desc);

create index if not exists idx_diligencia_certidao_workitem
    on tb_diligencia_operador_certidao (work_item_id, created_at desc);

create index if not exists idx_diligencia_certidao_checkpoint
    on tb_diligencia_operador_certidao (checkpoint_event_id);

create index if not exists idx_diligencia_certidao_digest
    on tb_diligencia_operador_certidao (certificate_digest_sha256);
