create index if not exists idx_work_item_processo_created_operacional
    on tb_work_item (processo_id, created_at desc, id desc)
    where status <> 'CANCELADO';

create index if not exists idx_work_item_assigned_due
    on tb_work_item (assigned_user_id, status, due_at);

create index if not exists idx_work_item_role_territory_due
    on tb_work_item (assigned_role, uf, comarca, status, due_at);

create index if not exists idx_processo_prazo_scan
    on tb_processo (id)
    where ramo_direito is not null
      and status_processo <> 'BAIXADO'
      and status_processo <> 'ARQUIVADO'
      and status_processo <> 'TRANSITO_EM_JULGADO';

create index if not exists idx_com_jud_state_domain_secondary_updated
    on tb_comunicacao_judicial_state (domain_name, secondary_key, updated_at desc);

create index if not exists idx_com_jud_state_domain_processo_updated
    on tb_comunicacao_judicial_state (domain_name, processo_id, updated_at desc);

create index if not exists idx_outbox_pending_available_created
    on tb_outbox_event (available_at, created_at)
    where status = 'PENDING';

create index if not exists idx_job_claim_hot
    on tb_job (next_retry_at, locked_at, priority desc, created_at)
    where status in ('PENDING', 'FAILED');
