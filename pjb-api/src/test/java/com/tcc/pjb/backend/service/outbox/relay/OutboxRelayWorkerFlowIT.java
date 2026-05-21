package com.tcc.pjb.backend.service.outbox.relay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.outbox.OutboxEvent;
import com.tcc.pjb.backend.model.entity.outbox.OutboxStatus;
import com.tcc.pjb.backend.repository.outbox.OutboxEventRepository;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestPropertySource(properties = {
        "pjb.outbox.relay.enabled=true",
        "pjb.outbox.relay.batch-size=10",
        "pjb.outbox.relay.max-attempts=2",
        "pjb.outbox.relay.send-timeout-ms=2000",
        "spring.cache.type=none",
        "pjb.workflow.enabled=false"
})
class OutboxRelayWorkerFlowIT extends PjbIntegrationTestBase {

    @Autowired
    private OutboxRelayWorker outboxRelayWorker;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void clearOutbox() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void deveDespacharEventoPendenteEMarcarComoDone() {
        given(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .willAnswer(inv -> CompletableFuture.completedFuture(null));

        OutboxEvent event = outboxEventRepository.save(new OutboxEvent(
                UUID.randomUUID(),
                "processo:relay-it-1",
                "pjb.processo.criado",
                "{\"processoId\":1}",
                Instant.now()
        ));

        outboxRelayWorker.relay();

        OutboxEvent result = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(OutboxStatus.DONE);
        assertThat(result.getLockedBy()).isNull();
        assertThat(result.getLockedAt()).isNull();
    }

    @Test
    void deveProgramarRetentativaQuandoKafkaFalhar() {
        given(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .willAnswer(inv -> {
                    CompletableFuture<Object> fut = new CompletableFuture<>();
                    fut.completeExceptionally(new RuntimeException("kafka-unavailable"));
                    return fut;
                });

        OutboxEvent event = outboxEventRepository.save(new OutboxEvent(
                UUID.randomUUID(),
                "processo:relay-it-2",
                "pjb.processo.criado",
                "{\"processoId\":2}",
                Instant.now()
        ));

        outboxRelayWorker.relay();

        OutboxEvent result = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(result.getAttempts()).isEqualTo(1);
        assertThat(result.getLastError()).contains("kafka-unavailable");
    }

    @Test
    void deveMarcarFalhaDefinitivaAposMaxTentativas() {
        given(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .willAnswer(inv -> {
                    CompletableFuture<Object> fut = new CompletableFuture<>();
                    fut.completeExceptionally(new RuntimeException("kafka-permanently-down"));
                    return fut;
                });

        OutboxEvent event = outboxEventRepository.save(new OutboxEvent(
                UUID.randomUUID(),
                "processo:relay-it-3",
                "pjb.processo.criado",
                "{\"processoId\":3}",
                Instant.now()
        ));
        event.markRetry(Instant.now(), "attempt-1");
        event.markRetry(Instant.now(), "attempt-2");
        outboxEventRepository.save(event);

        outboxRelayWorker.relay();

        OutboxEvent result = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(result.getLastError()).contains("kafka-permanently-down");
    }
}
