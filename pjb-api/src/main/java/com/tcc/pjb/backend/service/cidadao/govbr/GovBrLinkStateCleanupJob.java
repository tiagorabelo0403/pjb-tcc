package com.tcc.pjb.backend.service.cidadao.govbr;

import java.time.Instant;
import java.util.Objects;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.repository.GovBrLinkStateRepository;

@Component
public class GovBrLinkStateCleanupJob {

  private final GovBrLinkStateRepository repo;

  public GovBrLinkStateCleanupJob(GovBrLinkStateRepository repo) {
    this.repo = Objects.requireNonNull(repo);
  }

  @Scheduled(fixedDelayString = "${pjb.integrations.govbr.cleanup-interval:3600000}")
  @Transactional
  public void cleanup() {
    repo.deleteExpired(Instant.now());
  }
}
