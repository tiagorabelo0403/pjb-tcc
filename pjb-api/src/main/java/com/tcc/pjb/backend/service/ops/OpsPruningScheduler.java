package com.tcc.pjb.backend.service.ops;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OpsPruningScheduler {

  private static final Logger log = LoggerFactory.getLogger(OpsPruningScheduler.class);

  private final JdbcTemplate jdbc;
  private final boolean enabled;
  private final Duration retention;

  public OpsPruningScheduler(JdbcTemplate jdbc, Environment env) {
    this.jdbc = Objects.requireNonNull(jdbc);
    this.enabled = Boolean.parseBoolean(env.getProperty("pjb.ops.prune.enabled", "true"));
    long days = Long.parseLong(env.getProperty("pjb.ops.prune.retentionDays", "7"));
    this.retention = Duration.ofDays(Math.max(1, days));
  }

  @Scheduled(cron = "0 10 3 * * *")
  public void prune() {
    if (!enabled) {
      return;
    }

    Instant cutoff = Instant.now().minus(retention);
    Timestamp ts = Timestamp.from(cutoff);

    try {
      int outbox = jdbc.update(
          "delete from tb_outbox_event where status in ('DONE','FAILED') and created_at < ?",
          ts
      );
      int idem = jdbc.update(
          "delete from tb_idempotency where status = 'COMPLETED' and created_at < ?",
          ts
      );
      log.info("Prune ok: outbox={} idempotency={} retentionDays={}", outbox, idem, retention.toDays());
    } catch (Exception ex) {
      log.warn("Prune failed: {}", ex.getMessage());
    }
  }
}
