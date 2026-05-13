create table if not exists tb_diligencia_operador_encerramento (
    id bigserial primary key,
    operator_user_id bigint not null,
    operator_tipo_usuario varchar(80) not null,
    canal varchar(40) not null,
    diligence_reference varchar(120) not null,
    outcome varchar(40) not null,
    work_item_id bigint,
    processo_id bigint,
    processo_numero varchar(32),
    certidao_id bigint,
    checkpoint_event_id bigint,
    certidao_digest_sha256 varchar(64),
    work_item_status_final varchar(30),
    followup_work_item_id bigint,
    documentos_vinculados integer not null default 0,
    idempotency_key varchar(64) not null,
    execution_digest_sha256 varchar(64) not null,
    request_id varchar(80),
    created_at timestamptz not null default now()
);

create index if not exists idx_diligencia_encerramento_user_ref
    on tb_diligencia_operador_encerramento (operator_user_id, canal, diligence_reference, created_at desc);

create index if not exists idx_diligencia_encerramento_certidao
    on tb_diligencia_operador_encerramento (certidao_id);

create unique index if not exists idx_diligencia_encerramento_idempotency
    on tb_diligencia_operador_encerramento (operator_user_id, canal, diligence_reference, idempotency_key);

create table if not exists tb_diligencia_operador_certidao_documento (
    id bigserial primary key,
    certidao_id bigint not null,
    processo_id bigint not null,
    documento_id uuid not null,
    documento_titulo varchar(255),
    documento_sha256 varchar(64),
    origem varchar(20) not null,
    created_at timestamptz not null default now()
);

create index if not exists idx_diligencia_certidao_documento_certidao
    on tb_diligencia_operador_certidao_documento (certidao_id, created_at desc);

create index if not exists idx_diligencia_certidao_documento_doc
    on tb_diligencia_operador_certidao_documento (documento_id);

create unique index if not exists idx_diligencia_certidao_documento_unique
    on tb_diligencia_operador_certidao_documento (certidao_id, documento_id);
