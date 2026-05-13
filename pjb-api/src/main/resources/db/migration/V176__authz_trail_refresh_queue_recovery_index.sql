create index if not exists idx_authz_analytics_refresh_locked
    on tb_authz_trail_analytics_refresh_queue (status, locked_at);
