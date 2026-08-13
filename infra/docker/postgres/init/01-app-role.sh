#!/usr/bin/env bash
# Scripts em docker-entrypoint-initdb.d SO RODAM com PGDATA vazio (comportamento padrao da
# imagem oficial do Postgres) — um volume ja existente (ex.: pjb_pjb_pg_data de um ambiente dev
# anterior a este hardening) nunca executa este script, entao a role pjb_app nunca e criada
# nele, e o backend falha ao autenticar. Nao ha migracao automatica: para um volume antigo, rode
# manualmente o SQL abaixo (mesmo conteudo deste script) uma vez, com o Postgres do volume ja no
# ar:
#
#   docker exec -i <container_postgres> psql -v ON_ERROR_STOP=1 -U pjb -d pjb <<'SQL'
#   DO
#   $$
#   BEGIN
#       IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'pjb_app') THEN
#           CREATE ROLE pjb_app WITH LOGIN PASSWORD 'pjb_app_pass' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
#       ELSE
#           ALTER ROLE pjb_app WITH LOGIN PASSWORD 'pjb_app_pass' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
#       END IF;
#   END
#   $$;
#   GRANT CREATE ON DATABASE pjb TO pjb_app;
#   GRANT ALL PRIVILEGES ON SCHEMA public TO pjb_app;
#   GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO pjb_app;
#   GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO pjb_app;
#   ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO pjb_app;
#   ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO pjb_app;
#   SQL
#
# GRANT sozinho nao basta num volume onde migrations <= V313 ja rodaram como o superusuario
# antigo (pjb): DDL que muda o tipo de uma coluna existente (ex.: V317, ALTER COLUMN ... TYPE)
# exige que quem executa seja DONO da tabela, nao so ter privilegio via GRANT. Nesse volume,
# toda tabela criada antes da migracao para pjb_app ainda pertence a pjb, e o Flyway (rodando
# como pjb_app) falha com "must be owner of table" na primeira migration que altera uma coluna
# existente. Rode tambem o bloco abaixo, uma unica vez, antes do proximo boot do backend:
#
#   docker exec -i <container_postgres> psql -v ON_ERROR_STOP=1 -U pjb -d pjb <<'SQL'
#   DO
#   $$
#   DECLARE
#       tabela RECORD;
#   BEGIN
#       FOR tabela IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' LOOP
#           EXECUTE format('ALTER TABLE public.%I OWNER TO pjb_app', tabela.tablename);
#       END LOOP;
#   END
#   $$;
#   SQL
#
# NAO resolva isso com GRANT pjb_app TO pjb (tornar pjb_app membro da role pjb) — isso permite
# SET ROLE pjb dentro de uma sessao autenticada como pjb_app, reabrindo exatamente o bypass de
# RLS (superusuario ignora FORCE ROW LEVEL SECURITY) que a introducao de pjb_app existiu pra
# fechar. A troca de ownership acima e a unica forma segura: pjb_app passa a poder alterar as
# tabelas sem herdar nenhum privilegio de pjb.
#
# Troque 'pjb_app_pass', 'pjb' (usuario/db de conexao) e a role/senha de destino se o seu .env
# usa valores diferentes de PJB_DB_APP_USER/PASS. Ver README.md, seção "Banco de dados", para o
# contexto completo desta role. Depois de rodar o SQL acima, reinicie o backend.
set -euo pipefail
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
    -v app_user="$PJB_DB_APP_USER" -v app_pass="$PJB_DB_APP_PASS" -v db_name="$POSTGRES_DB" <<'SQL'
-- psql so faz substituicao de :'var'/:"var" fora de blocos dollar-quoted ($$...$$) — por isso a
-- criacao idempotente da role usa \gexec (gera o CREATE/ALTER ROLE como texto puro, sem DO $$)
-- em vez de um bloco PL/pgSQL, o que também evita interpolar a senha direto num literal SQL.
SELECT format('CREATE ROLE %I WITH LOGIN PASSWORD %L NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE', :'app_user', :'app_pass')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'app_user')
UNION ALL
SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE', :'app_user', :'app_pass')
WHERE EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'app_user')
\gexec

-- CREATE em nivel de banco (nao so de schema) e exigido pelo Postgres para instalar extensoes
-- "trusted" (pg_trgm, btree_gist, vector) sem superusuario — migrations V219/V257/V302/V307
-- rodam CREATE EXTENSION IF NOT EXISTS e falham com 42501 sem este grant.
GRANT CREATE ON DATABASE :"db_name" TO :"app_user";
GRANT ALL PRIVILEGES ON SCHEMA public TO :"app_user";
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO :"app_user";
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO :"app_user";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO :"app_user";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO :"app_user";
SQL
