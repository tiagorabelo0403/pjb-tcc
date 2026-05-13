package com.tcc.pjb.backend.service.ui.accessibility.cleanup;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.repository.ui.AccessibilityUsageSnapshotRepository;
import com.tcc.pjb.backend.service.ui.accessibility.AccessibilityProperties;

@Component
public class UiAccessibilitySnapshotCleanupScheduler {

  private static final Logger log = LoggerFactory.getLogger(UiAccessibilitySnapshotCleanupScheduler.class);

  private final AccessibilityProperties props;
  private final AccessibilityUsageSnapshotRepository repo;

  public UiAccessibilitySnapshotCleanupScheduler(AccessibilityProperties props,
                                                 AccessibilityUsageSnapshotRepository repo) {
    this.props = Objects.requireNonNull(props);
    this.repo = Objects.requireNonNull(repo);
  }

  @Scheduled(cron = "0 25 3 * * *")
  @Transactional
  public void cleanup() {
    int days = Math.max(30, props.getSnapshotRetentionDays());
    Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
    try {
      int removed = repo.deleteOlderThan(cutoff);
      if (removed > 0) {
        log.info("ui accessibility snapshots cleanup removed={} cutoff={}", removed, cutoff);
      }
    } catch (Exception ex) {
      log.debug("ui accessibility cleanup failed: {}", ex.getMessage());
    }
  }
}
