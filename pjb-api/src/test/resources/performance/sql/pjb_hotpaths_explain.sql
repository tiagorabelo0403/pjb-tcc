begin;

explain analyze
select w.id
from tb_work_item w
where w.processo_id = 1
  and w.status <> 'CANCELADO'
order by w.created_at desc, w.id desc
limit 1;

explain analyze
select p.id
from tb_processo p
where p.ramo_direito is not null
  and p.status_processo <> 'BAIXADO'
  and p.status_processo <> 'ARQUIVADO'
  and p.status_processo <> 'TRANSITO_EM_JULGADO'
order by p.id asc
limit 200;

explain analyze
select payload_json
from tb_comunicacao_judicial_state
where domain_name = 'QR_MANDADO_ATIVO'
  and secondary_key = 'TOKEN'
order by updated_at desc;

explain analyze
select id
from tb_outbox_event
where status = 'PENDING'
  and available_at <= now()
order by created_at asc
limit 50;

explain analyze
with cte as (
    select id
    from tb_job
    where status in ('PENDING', 'FAILED')
      and (next_retry_at is null or next_retry_at <= now())
      and (locked_at is null or locked_at <= now() - interval '30 seconds')
    order by priority desc, created_at asc
    limit 50
)
select * from cte;

rollback;
