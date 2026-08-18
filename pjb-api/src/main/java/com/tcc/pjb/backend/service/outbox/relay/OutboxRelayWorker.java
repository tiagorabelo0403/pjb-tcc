package com.tcc.pjb.backend.service.outbox.relay;

import com.tcc.pjb.backend.model.entity.outbox.OutboxEvent;
import com.tcc.pjb.backend.model.entity.outbox.OutboxEventId;
import com.tcc.pjb.backend.model.entity.outbox.OutboxStatus;
import com.tcc.pjb.backend.repository.outbox.OutboxEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty(prefix = "pjb.outbox.relay", name = "enabled", havingValue = "true")
@ConditionalOnProperty(name = "pjb.kafka.enabled", havingValue = "true")
public class OutboxRelayWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayWorker.class);

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxRelayProperties properties;
    private final TransactionTemplate tx;
    private final String workerId;

    public OutboxRelayWorker(OutboxEventRepository repository,
                             KafkaTemplate<String, String> kafkaTemplate,
                             OutboxRelayProperties properties,
                             PlatformTransactionManager transactionManager) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate, "kafkaTemplate");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.tx = new TransactionTemplate(Objects.requireNonNull(transactionManager, "transactionManager"));
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.workerId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    @Scheduled(fixedDelayString = "${pjb.outbox.relay.poll-interval:500ms}")
    public void relay() {
        List<OutboxEventId> claimed = claimBatch();
        for (OutboxEventId eventId : claimed) {
            dispatch(eventId);
        }
    }

    private List<OutboxEventId> claimBatch() {
        List<OutboxEventId> result = tx.execute(status -> {
            List<Object[]> rows = repository.claimRawForUpdate(
                    OutboxStatus.PENDING.name(), properties.batchSize());
            if (rows.isEmpty()) {
                return List.of();
            }
            List<OutboxEventId> keys = rows.stream()
                    .map(OutboxEventId::fromRow)
                    .toList();
            List<OutboxEvent> events = repository.findAllById(keys);
            Instant now = Instant.now();
            for (OutboxEvent event : events) {
                event.markInflight(workerId, now);
            }
            repository.saveAll(events);
            return keys;
        });
        return result != null ? result : List.of();
    }

    private void dispatch(OutboxEventId eventId) {
        tx.execute(status -> {
            OutboxEvent event = repository.findById(eventId).orElse(null);
            if (event == null || event.getStatus() != OutboxStatus.INFLIGHT) {
                return null;
            }
            try {
                kafkaTemplate.send(
                        event.getRoutingKey(),
                        event.getId().toString(),
                        event.getPayloadJson()
                ).get(properties.sendTimeoutMs(), TimeUnit.MILLISECONDS);
                event.markDone();
                log.debug("[OUTBOX] dispatched id={} type={}", eventId.getId(), event.getEventType());
            } catch (Exception e) {
                if (event.getAttempts() >= properties.maxAttempts()) {
                    event.markFailed(truncate(e.getMessage()));
                    log.warn("[OUTBOX] failed permanently id={} type={} err={}", eventId.getId(), event.getEventType(), e.getMessage());
                } else {
                    event.markRetry(Instant.now().plus(properties.retryBackoff()), truncate(e.getMessage()));
                    log.debug("[OUTBOX] scheduled retry id={} attempt={}", eventId.getId(), event.getAttempts());
                }
            }
            repository.save(event);
            return null;
        });
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
