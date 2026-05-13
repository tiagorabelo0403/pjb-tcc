create index if not exists idx_outbox_failed_created
    on tb_outbox_event (created_at)
    where status = 'FAILED';

create index if not exists idx_outbox_inflight_locked
    on tb_outbox_event (locked_at, created_at)
    where status = 'INFLIGHT';

create index if not exists idx_outbox_pending_event_type_available
    on tb_outbox_event (event_type, available_at, created_at)
    where status = 'PENDING';
