package com.tcc.pjb.backend.service.ui.cleanup;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.repository.ui.UiStateHistoryRepository;

@Component
public class UiHistoryRetentionScheduler {

  private static final Logger log = LoggerFactory.getLogger(UiHistoryRetentionScheduler.class);

  private final UiStateHistoryRepository repo;
  private final boolean enabled;
  private final int retentionDays;

  public UiHistoryRetentionScheduler(UiStateHistoryRepository repo, Environment env) {
    this.repo = Objects.requireNonNull(repo);
    this.enabled = Boolean.parseBoolean(env.getProperty("pjb.ui.history.retention.enabled", "true"));
    this.retentionDays = Math.max(30, Integer.parseInt(env.getProperty("pjb.ui.history.retention.days", "365")));
  }

  @Scheduled(cron = "${pjb.ui.history.retention.cron:0 18 3 * * *}")
  @Transactional
  public void prune() {
    if (!enabled) return;

    Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
    try {
      int removed = repo.deleteOlderThan(cutoff);
      if (removed > 0) {
        log.info("ui history retention removed={} cutoff={}", removed, cutoff);
      }
    } catch (Exception ex) {
      log.debug("ui history retention failed: {}", ex.getMessage());
    }
  }
}
