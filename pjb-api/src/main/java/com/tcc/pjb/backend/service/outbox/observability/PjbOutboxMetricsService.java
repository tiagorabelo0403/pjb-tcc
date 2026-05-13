package com.tcc.pjb.backend.service.outbox.observability;

import com.tcc.pjb.backend.model.entity.outbox.OutboxStatus;
import com.tcc.pjb.backend.repository.outbox.OutboxEventRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pjb.outbox.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PjbOutboxMetricsService {

    private final OutboxEventRepository repository;
    private final AtomicLong pending = new AtomicLong();
    private final AtomicLong inflight = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong pendingOldestAgeSeconds = new AtomicLong();
    private final AtomicLong inflightOldestAgeSeconds = new AtomicLong();
    private final AtomicLong ingressDepth = new AtomicLong();
    private final AtomicLong directPersistCount = new AtomicLong();
    private final AtomicLong rejectedPersistCount = new AtomicLong();
    private final com.tcc.pjb.backend.service.outbox.OutboxPublisher publisher;

    public PjbOutboxMetricsService(OutboxEventRepository repository,
                                   MeterRegistry meterRegistry,
                                   com.tcc.pjb.backend.service.outbox.OutboxPublisher publisher) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        Gauge.builder("pjb.outbox.pending", pending, AtomicLong::get).register(meterRegistry);
        Gauge.builder("pjb.outbox.inflight", inflight, AtomicLong::get).register(meterRegistry);
        Gauge.builder("pjb.outbox.failed", failed, AtomicLong::get).register(meterRegistry);
        Gauge.builder("pjb.outbox.pending.oldest.age.seconds", pendingOldestAgeSeconds, AtomicLong::get).register(meterRegistry);
        Gauge.builder("pjb.outbox.inflight.oldest.age.seconds", inflightOldestAgeSeconds, AtomicLong::get).register(meterRegistry);
        Gauge.builder("pjb.outbox.ingress.depth", ingressDepth, AtomicLong::get).register(meterRegistry);
        Gauge.builder("pjb.outbox.ingress.direct.persist", directPersistCount, AtomicLong::get).register(meterRegistry);
        Gauge.builder("pjb.outbox.ingress.rejected", rejectedPersistCount, AtomicLong::get).register(meterRegistry);
        refresh();
    }

    @Scheduled(fixedDelayString = "${pjb.outbox.observability.refresh-interval:30s}")
    public void refresh() {
        pending.set(repository.countByStatus(OutboxStatus.PENDING));
        inflight.set(repository.countByStatus(OutboxStatus.INFLIGHT));
        failed.set(repository.countByStatus(OutboxStatus.FAILED));
        pendingOldestAgeSeconds.set(ageSeconds(repository.findOldestCreatedAtByStatus(OutboxStatus.PENDING)));
        inflightOldestAgeSeconds.set(ageSeconds(repository.findOldestLockedAtByStatus(OutboxStatus.INFLIGHT)));
        ingressDepth.set(publisher.ingressDepth());
        directPersistCount.set(publisher.directPersistCount());
        rejectedPersistCount.set(publisher.rejectedPersistCount());
    }

    private static long ageSeconds(Instant instant) {
        if (instant == null) {
            return 0L;
        }
        return Math.max(0L, Duration.between(instant, Instant.now()).getSeconds());
    }

    public long pending() {
        return pending.get();
    }

    public long inflight() {
        return inflight.get();
    }

    public long failed() {
        return failed.get();
    }

    public long pendingOldestAgeSeconds() {
        return pendingOldestAgeSeconds.get();
    }

    public long inflightOldestAgeSeconds() {
        return inflightOldestAgeSeconds.get();
    }

    public long ingressDepth() {
        return ingressDepth.get();
    }
}
