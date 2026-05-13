FROM debian:bookworm-slim
WORKDIR /opt/pgbouncer
RUN apt-get update \
    && apt-get install -y --no-install-recommends pgbouncer postgresql-client ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --home /var/lib/pgbouncer --create-home --shell /usr/sbin/nologin pgbouncer \
    && mkdir -p /etc/pgbouncer /var/log/pgbouncer \
    && chown -R pgbouncer:pgbouncer /etc/pgbouncer /var/lib/pgbouncer /var/log/pgbouncer
COPY entrypoint.sh /usr/local/bin/pjb-pgbouncer-entrypoint.sh
RUN chmod +x /usr/local/bin/pjb-pgbouncer-entrypoint.sh
USER pgbouncer
EXPOSE 6432
ENTRYPOINT ["/usr/local/bin/pjb-pgbouncer-entrypoint.sh"]
