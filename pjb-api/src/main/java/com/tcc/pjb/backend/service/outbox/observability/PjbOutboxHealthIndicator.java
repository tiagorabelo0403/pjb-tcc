package com.tcc.pjb.backend.service.outbox.observability;

import com.tcc.pjb.backend.model.entity.outbox.OutboxStatus;
import com.tcc.pjb.backend.repository.outbox.OutboxEventRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pjb.outbox.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PjbOutboxHealthIndicator implements HealthIndicator {

    private final OutboxEventRepository repository;
    private final PjbOutboxObservabilityProperties properties;
    private final com.tcc.pjb.backend.service.outbox.OutboxPublisher publisher;

    public PjbOutboxHealthIndicator(OutboxEventRepository repository,
                                    PjbOutboxObservabilityProperties properties,
                                    com.tcc.pjb.backend.service.outbox.OutboxPublisher publisher) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
    }

    @Override
    public Health health() {
        long pending = repository.countByStatus(OutboxStatus.PENDING);
        long inflight = repository.countByStatus(OutboxStatus.INFLIGHT);
        long failed = repository.countByStatus(OutboxStatus.FAILED);
        long pendingAgeSeconds = ageSeconds(repository.findOldestCreatedAtByStatus(OutboxStatus.PENDING));
        long inflightAgeSeconds = ageSeconds(repository.findOldestLockedAtByStatus(OutboxStatus.INFLIGHT));
        int ingressDepth = publisher.ingressDepth();
        long directPersistCount = publisher.directPersistCount();
        long rejectedPersistCount = publisher.rejectedPersistCount();

        boolean failedOverflow = failed >= properties.getFailedThreshold();
        boolean pendingStale = pendingAgeSeconds >= properties.getPendingLagAfter().getSeconds();
        boolean inflightStale = inflightAgeSeconds >= properties.getInflightStaleAfter().getSeconds();

        Health.Builder builder = failedOverflow || pendingStale || inflightStale ? Health.down() : Health.up();
        return builder
                .withDetail("pending", pending)
                .withDetail("inflight", inflight)
                .withDetail("failed", failed)
                .withDetail("pendingOldestAgeSeconds", pendingAgeSeconds)
                .withDetail("inflightOldestAgeSeconds", inflightAgeSeconds)
                .withDetail("ingressDepth", ingressDepth)
                .withDetail("directPersistCount", directPersistCount)
                .withDetail("rejectedPersistCount", rejectedPersistCount)
                .withDetail("failedThreshold", properties.getFailedThreshold())
                .withDetail("pendingLagAfterSeconds", properties.getPendingLagAfter().getSeconds())
                .withDetail("inflightStaleAfterSeconds", properties.getInflightStaleAfter().getSeconds())
                .build();
    }

    private static long ageSeconds(Instant instant) {
        if (instant == null) {
            return 0L;
        }
        return Math.max(0L, Duration.between(instant, Instant.now()).getSeconds());
    }
}
