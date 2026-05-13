create table if not exists tb_diligencia_operador_anexacao_institucional (
    id bigserial primary key,
    operator_user_id bigint not null,
    operator_tipo_usuario varchar(80) not null,
    canal varchar(40) not null,
    diligence_reference varchar(120) not null,
    work_item_id bigint null,
    processo_id bigint not null,
    processo_numero varchar(32) null,
    juntada_id bigint not null,
    formalizacao_id bigint null,
    encerramento_id bigint null,
    certidao_id bigint null,
    pacote_documento_id uuid null,
    bundle_reference varchar(160) null,
    bundle_digest_sha256 varchar(64) not null,
    bundle_signature_hmac_sha256 varchar(64) not null,
    external_system_code varchar(40) not null,
    destination_box varchar(160) not null,
    ack_protocol varchar(120) not null,
    ack_reference varchar(160) not null,
    annexation_status varchar(40) not null,
    external_receipt_digest_sha256 varchar(64) not null,
    chain_idempotency_key varchar(64) not null,
    process_event_seq bigint null,
    request_hash_sha256 varchar(64) not null,
    execution_digest_sha256 varchar(64) not null,
    observacoes varchar(3000) null,
    externalized_at timestamptz null,
    request_id varchar(80) null,
    created_at timestamptz not null default now()
);

create index if not exists idx_diligencia_anexacao_user_ref on tb_diligencia_operador_anexacao_institucional(operator_user_id, canal, diligence_reference, created_at desc);
create index if not exists idx_diligencia_anexacao_processo on tb_diligencia_operador_anexacao_institucional(processo_id, created_at desc);
create index if not exists idx_diligencia_anexacao_juntada on tb_diligencia_operador_anexacao_institucional(juntada_id);
create index if not exists idx_diligencia_anexacao_ack on tb_diligencia_operador_anexacao_institucional(external_system_code, ack_protocol);
create unique index if not exists uk_diligencia_anexacao_chain_key on tb_diligencia_operador_anexacao_institucional(operator_user_id, canal, diligence_reference, chain_idempotency_key);
