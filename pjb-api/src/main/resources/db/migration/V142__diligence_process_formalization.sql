create table if not exists tb_diligencia_operador_formalizacao_processual (
    id bigserial primary key,
    operator_user_id bigint not null,
    operator_tipo_usuario varchar(80) not null,
    canal varchar(40) not null,
    diligence_reference varchar(120) not null,
    work_item_id bigint,
    processo_id bigint not null,
    processo_numero varchar(32),
    encerramento_id bigint,
    certidao_id bigint not null,
    checkpoint_event_id bigint,
    movimentacao_id bigint,
    movimentacao_event_seq bigint,
    minuta_documento_id uuid,
    minuta_event_seq bigint,
    minuta_titulo varchar(255),
    minuta_sha256 varchar(64),
    minuta_sha384 varchar(96),
    certidao_digest_sha256 varchar(64),
    evidence_chave_custodia varchar(32),
    evidence_integrity_ok boolean,
    documentos_referenciados integer not null default 0,
    idempotency_key varchar(64) not null,
    formalization_digest_sha256 varchar(64) not null,
    request_id varchar(80),
    created_at timestamptz not null default now()
);

create index if not exists idx_diligencia_formalizacao_user_ref
    on tb_diligencia_operador_formalizacao_processual (operator_user_id, canal, diligence_reference, created_at desc);

create index if not exists idx_diligencia_formalizacao_processo
    on tb_diligencia_operador_formalizacao_processual (processo_id, created_at desc);

create index if not exists idx_diligencia_formalizacao_certidao
    on tb_diligencia_operador_formalizacao_processual (certidao_id);

create unique index if not exists idx_diligencia_formalizacao_idempotencia
    on tb_diligencia_operador_formalizacao_processual (operator_user_id, canal, diligence_reference, idempotency_key);
