package com.tcc.pjb.backend.modules.atendimento.service;

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
public class AtendimentoModerationRetentionScheduler {

  private static final Logger log = LoggerFactory.getLogger(AtendimentoModerationRetentionScheduler.class);

  private final JdbcTemplate jdbc;
  private final boolean enabled;
  private final Duration retention;

  public AtendimentoModerationRetentionScheduler(JdbcTemplate jdbc, Environment env) {
    this.jdbc = Objects.requireNonNull(jdbc);
    this.enabled = Boolean.parseBoolean(env.getProperty("pjb.atendimento.moderation.retention.enabled", "true"));
    long days = Long.parseLong(env.getProperty("pjb.atendimento.moderation.retention.days", "180"));
    this.retention = Duration.ofDays(Math.max(30, days));
  }

  @Scheduled(cron = "${pjb.atendimento.moderation.retention.cron:0 20 3 * * *}")
  public void prune() {
    if (!enabled) return;

    Instant cutoff = Instant.now().minus(retention);
    try {
      int removed = jdbc.update(
          "delete from tb_atendimento_moderation_event where created_at < ?",
          Timestamp.from(cutoff)
      );
      if (removed > 0) {
        log.info("atendimento moderation retention removed={} cutoff={}", removed, cutoff);
      }
    } catch (Exception ex) {
      log.debug("atendimento moderation retention failed: {}", ex.getMessage());
    }
  }
}
