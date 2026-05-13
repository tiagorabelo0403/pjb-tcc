CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER SYSTEM SET track_io_timing = 'on';
ALTER SYSTEM SET compute_query_id = 'auto';
