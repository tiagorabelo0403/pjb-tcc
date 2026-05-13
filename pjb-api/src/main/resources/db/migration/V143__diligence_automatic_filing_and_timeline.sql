create table if not exists tb_diligencia_operador_juntada_processual (
    id bigserial primary key,
    operator_user_id bigint not null,
    operator_tipo_usuario varchar(80) not null,
    canal varchar(40) not null,
    diligence_reference varchar(120) not null,
    work_item_id bigint,
    processo_id bigint not null,
    processo_numero varchar(32),
    formalizacao_id bigint not null,
    encerramento_id bigint,
    certidao_id bigint,
    movimentacao_id bigint,
    movimentacao_event_seq bigint,
    pacote_documento_id uuid,
    pacote_event_seq bigint,
    minuta_documento_id uuid,
    pacote_titulo varchar(255),
    pacote_sha256 varchar(64),
    certidao_digest_sha256 varchar(64),
    formalization_digest_sha256 varchar(64),
    evidence_chave_custodia varchar(32),
    evidence_integrity_ok boolean,
    documentos_referenciados integer not null default 0,
    exportar_malha_externa boolean not null default false,
    external_system_code varchar(40),
    bundle_reference varchar(160),
    bundle_digest_sha256 varchar(64) not null,
    bundle_signature_hmac_sha256 varchar(64) not null,
    idempotency_key varchar(64) not null,
    request_id varchar(80),
    created_at timestamptz not null default now()
);

create index if not exists idx_diligencia_juntada_user_ref
    on tb_diligencia_operador_juntada_processual (operator_user_id, canal, diligence_reference, created_at desc);

create index if not exists idx_diligencia_juntada_processo
    on tb_diligencia_operador_juntada_processual (processo_id, created_at desc);

create index if not exists idx_diligencia_juntada_formalizacao
    on tb_diligencia_operador_juntada_processual (formalizacao_id);

create unique index if not exists idx_diligencia_juntada_idempotencia
    on tb_diligencia_operador_juntada_processual (operator_user_id, canal, diligence_reference, idempotency_key);
