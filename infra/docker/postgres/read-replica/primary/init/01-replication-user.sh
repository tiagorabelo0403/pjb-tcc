#!/usr/bin/env bash
set -euo pipefail
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<SQL
DO
\$\$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${PJB_REPL_USER}') THEN
        EXECUTE format('CREATE ROLE %I WITH REPLICATION LOGIN PASSWORD %L', '${PJB_REPL_USER}', '${PJB_REPL_PASS}');
    ELSE
        EXECUTE format('ALTER ROLE %I WITH REPLICATION LOGIN PASSWORD %L', '${PJB_REPL_USER}', '${PJB_REPL_PASS}');
    END IF;
END
\$\$;
SQL
cat >> "$PGDATA/pg_hba.conf" <<EOF_HBA
host replication ${PJB_REPL_USER} all scram-sha-256
host all ${PJB_DB_USER:-pjb} all scram-sha-256
EOF_HBA
