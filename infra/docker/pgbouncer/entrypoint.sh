#!/bin/sh
set -eu
DB_NAME="${PJB_DB_NAME:-pjb}"
DB_HOST="${PJB_PGBOUNCER_DB_HOST:-postgres}"
DB_PORT="${PJB_PGBOUNCER_DB_PORT:-5432}"
DB_USER="${PJB_PGBOUNCER_DB_USER:-${PJB_DB_USER:-pjb}}"
DB_PASS="${PJB_PGBOUNCER_DB_PASS:-${PJB_DB_PASS:-pjb}}"
LISTEN_PORT="${PJB_PGBOUNCER_PORT:-6432}"
POOL_MODE="${PJB_PGBOUNCER_POOL_MODE:-transaction}"
MAX_CLIENT_CONN="${PJB_PGBOUNCER_MAX_CLIENT_CONN:-4000}"
DEFAULT_POOL_SIZE="${PJB_PGBOUNCER_DEFAULT_POOL_SIZE:-120}"
RESERVE_POOL_SIZE="${PJB_PGBOUNCER_RESERVE_POOL_SIZE:-20}"
RESERVE_POOL_TIMEOUT="${PJB_PGBOUNCER_RESERVE_POOL_TIMEOUT:-5}"
IGNORE_STARTUP="${PJB_PGBOUNCER_IGNORE_STARTUP_PARAMETERS:-extra_float_digits,options,search_path}"
ADMIN_USERS="${PJB_PGBOUNCER_ADMIN_USERS:-$DB_USER}"
STATS_USERS="${PJB_PGBOUNCER_STATS_USERS:-$DB_USER}"
AUTH_TYPE="${PJB_PGBOUNCER_AUTH_TYPE:-plain}"
MAX_DB_CONNECTIONS="${PJB_PGBOUNCER_MAX_DB_CONNECTIONS:-0}"
SERVER_RESET_QUERY_ALWAYS="${PJB_PGBOUNCER_SERVER_RESET_ALWAYS:-1}"
cat > /etc/pgbouncer/userlist.txt <<EOF
"$DB_USER" "$DB_PASS"
EOF
cat > /etc/pgbouncer/pgbouncer.ini <<EOF
[databases]
$DB_NAME = host=$DB_HOST port=$DB_PORT dbname=$DB_NAME user=$DB_USER password=$DB_PASS

[pgbouncer]
listen_addr = 0.0.0.0
listen_port = $LISTEN_PORT
auth_type = $AUTH_TYPE
auth_file = /etc/pgbouncer/userlist.txt
admin_users = $ADMIN_USERS
stats_users = $STATS_USERS
pool_mode = $POOL_MODE
max_db_connections = $MAX_DB_CONNECTIONS
server_reset_query_always = $SERVER_RESET_QUERY_ALWAYS
max_client_conn = $MAX_CLIENT_CONN
default_pool_size = $DEFAULT_POOL_SIZE
reserve_pool_size = $RESERVE_POOL_SIZE
reserve_pool_timeout = $RESERVE_POOL_TIMEOUT
server_connect_timeout = 5
server_login_retry = 5
query_wait_timeout = 120
server_check_query = select 1
server_check_delay = 30
ignore_startup_parameters = $IGNORE_STARTUP
log_connections = 0
log_disconnections = 0
EOF
exec pgbouncer /etc/pgbouncer/pgbouncer.ini
