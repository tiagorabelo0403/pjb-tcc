#!/usr/bin/env bash
set -euo pipefail
PGDATA="${PGDATA:-/var/lib/postgresql/data}"
PRIMARY_HOST="${PJB_DB_PRIMARY_HOST:-postgres}"
PRIMARY_PORT="${PJB_DB_PRIMARY_PORT:-5432}"
REPL_USER="${PJB_REPL_USER:-replicator}"
REPL_PASS="${PJB_REPL_PASS:-replicator}"
if [ ! -s "$PGDATA/PG_VERSION" ]; then
  rm -rf "$PGDATA"/*
  until pg_isready -h "$PRIMARY_HOST" -p "$PRIMARY_PORT" -U "$REPL_USER" >/dev/null 2>&1; do
    sleep 2
  done
  export PGPASSWORD="$REPL_PASS"
  pg_basebackup -h "$PRIMARY_HOST" -p "$PRIMARY_PORT" -D "$PGDATA" -U "$REPL_USER" -Fp -Xs -P -R
  chmod 700 "$PGDATA"
fi
CONFIG_FILE="${PJB_DB_POSTGRES_CONFIG:-/etc/postgresql/postgresql-pjb.conf}"
if [ -f "$CONFIG_FILE" ]; then
  exec docker-entrypoint.sh postgres -c config_file="$CONFIG_FILE" -c hot_standby=on -c listen_addresses='*'
fi
exec docker-entrypoint.sh postgres -c hot_standby=on -c listen_addresses='*'
