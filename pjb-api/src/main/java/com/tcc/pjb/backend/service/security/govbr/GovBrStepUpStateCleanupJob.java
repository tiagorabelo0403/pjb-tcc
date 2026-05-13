package com.tcc.pjb.backend.service.security.govbr;

import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import java.time.Instant;
import java.util.Objects;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.repository.GovBrStepUpStateRepository;

@Component
public class GovBrStepUpStateCleanupJob {

  private final GovBrStepUpStateRepository repo;

  public GovBrStepUpStateCleanupJob(GovBrStepUpStateRepository repo) {
    this.repo = Objects.requireNonNull(repo);
  }

  @Scheduled(fixedDelayString = "${pjb.integrations.govbr.cleanup-interval:3600000}")
  @Transactional
  @PjbTransactionalBudget(operation = "govbr.stepup.cleanup.persist", maxMillis = 1500, critical = true)
  public void cleanup() {
    repo.deleteExpired(Instant.now());
  }
}