package com.tcc.pjb.backend.service.outbox;

import java.time.Duration;
import java.time.Instant;
import com.tcc.pjb.backend.model.entity.outbox.OutboxEventId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import com.tcc.pjb.backend.platform.cluster.PjbClusterSingletonTask;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.tcc.pjb.backend.model.entity.outbox.OutboxEvent;
import com.tcc.pjb.backend.repository.outbox.OutboxEventRepository;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleRuntimePolicyService;

@Component
public class OutboxPollerScheduler {

  private static final Logger log = LoggerFactory.getLogger(OutboxPollerScheduler.class);

  private final OutboxEventRepository repo;
  private final OutboxDispatcher dispatcher;
  private final TransactionTemplate tx;
  private final JudicialScaleRuntimePolicyService runtimePolicyService;
  private final String instanceId;

  private final boolean enabled;
  private final int batchSize;
  private final int maxAttempts;

  public OutboxPollerScheduler(
      OutboxEventRepository repo,
      OutboxDispatcher dispatcher,
      PlatformTransactionManager txm,
      JudicialScaleRuntimePolicyService runtimePolicyService,
      Environment env
  ) {
    this.repo = Objects.requireNonNull(repo);
    this.dispatcher = Objects.requireNonNull(dispatcher);
    this.tx = new TransactionTemplate(Objects.requireNonNull(txm));
    this.runtimePolicyService = Objects.requireNonNull(runtimePolicyService);

    this.enabled = Boolean.parseBoolean(env.getProperty("pjb.outbox.poller.enabled", "true"));
    this.batchSize = Integer.parseInt(env.getProperty("pjb.outbox.poller.batch", "50"));
    this.maxAttempts = Integer.parseInt(env.getProperty("pjb.outbox.poller.maxAttempts", "12"));
    this.instanceId = env.getProperty("pjb.instanceId", UUID.randomUUID().toString());
  }

  @PjbClusterSingletonTask(key = "outbox-poller", ttl = "PT30S")
  @Scheduled(fixedDelayString = "${pjb.outbox.poller.delayMs:500}")
  public void tick() {
    if (!enabled) {
      return;
    }

    Instant now = Instant.now();

    List<OutboxEvent> claimed = java.util.Objects.requireNonNullElseGet(tx.execute(status -> {
      List<Object[]> rawRows = repo.claimRawForUpdate("PENDING", Math.max(1, batchSize * 4));
      if (rawRows == null || rawRows.isEmpty()) {
        return List.of();
      }
      List<OutboxEventId> ids = rawRows.stream().map(OutboxEventId::fromRow).toList();
      List<OutboxEvent> events = repo.findAllById(ids);
      if (events.isEmpty()) {
        return List.of();
      }
      events.sort(Comparator
          .comparingDouble((OutboxEvent event) -> runtimePolicyService.priorityWeight(event)).reversed()
          .thenComparing(OutboxEvent::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparingInt(OutboxEvent::getAttempts));
      int claimLimit = Math.max(1, Math.min(events.size(), runtimePolicyService.outboxClaimBatch(events.get(0))));
      List<OutboxEvent> selected = events.stream().limit(claimLimit).toList();
      for (OutboxEvent event : selected) {
        event.markInflight(instanceId, now);
      }
      repo.saveAll(selected);
      return List.copyOf(selected);
    }), List::of);

    if (claimed.isEmpty()) {
      return;
    }

    for (OutboxEvent e : claimed) {
      processOne(e);
    }
  }

  private void processOne(OutboxEvent snapshot) {
    try {
      dispatcher.dispatch(snapshot);
      tx.execute(status -> {
        repo.findById(new OutboxEventId(snapshot.getId(), snapshot.getCreatedMonth())).ifPresent(ev -> {
          ev.markDone();
          repo.save(ev);
        });
        return null;
      });
    } catch (Exception ex) {
      log.warn("Outbox dispatch failed type={} id={} attempts={}: {}",
          snapshot.getEventType(), snapshot.getId(), snapshot.getAttempts(), ex.getMessage());

      tx.execute(status -> {
        repo.findById(new OutboxEventId(snapshot.getId(), snapshot.getCreatedMonth())).ifPresent(ev -> {
          int nextAttempts = ev.getAttempts() + 1;
          String err = truncate(ex.toString(), 500);
          if (nextAttempts >= maxAttempts) {
            ev.markFailed(err);
          } else {
            ev.markRetry(Instant.now().plus(backoff(nextAttempts)), err);
          }
          repo.save(ev);
        });
        return null;
      });
    }
  }

  private static Duration backoff(int attempts) {
    
    long baseMs = 1_000L;
    long ms;
    try {
      ms = Math.multiplyExact(baseMs, 1L << Math.min(20, attempts));
    } catch (ArithmeticException overflow) {
      ms = 300_000L;
    }
    ms = Math.min(ms, 300_000L);
    
    ms += (long) (Math.random() * 250L);
    return Duration.ofMillis(ms);
  }

  private static String truncate(String s, int max) {
    if (s == null) return null;
    if (s.length() <= max) return s;
    return s.substring(0, max);
  }
}
