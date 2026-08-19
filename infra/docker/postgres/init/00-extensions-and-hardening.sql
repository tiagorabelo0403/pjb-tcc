CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Pre-instaladas aqui (como superusuario, unica vez, no boot do volume) porque a role de
-- aplicacao pjb_app (01-app-role.sh) e NOSUPERUSER: pg_trgm/btree_gist sao "trusted" e ate
-- funcionariam so com GRANT CREATE ON DATABASE, mas a extensao vector NAO e trusted nesta
-- imagem — "must be superuser to create this extension" mesmo com CREATE ON DATABASE. As
-- migrations V219/V257 (pg_trgm), V302 (btree_gist) e V307 (vector) usam CREATE EXTENSION
-- IF NOT EXISTS, entao com a extensao ja presente elas viram no-op para pjb_app.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE EXTENSION IF NOT EXISTS vector;

ALTER SYSTEM SET track_io_timing = 'on';
ALTER SYSTEM SET compute_query_id = 'auto';
