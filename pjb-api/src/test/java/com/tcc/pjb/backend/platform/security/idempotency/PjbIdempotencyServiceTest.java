package com.tcc.pjb.backend.platform.security.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyReplayPayload;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class PjbIdempotencyServiceTest {

    @Test
    void shouldPersistReplayPayloadAndExposeReplayReadyStatus() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        Map<String, String> store = new ConcurrentHashMap<>();
        when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0, String.class);
            String value = invocation.getArgument(1, String.class);
            return store.putIfAbsent(key, value) == null;
        });
        when(ops.get(anyString())).thenAnswer(invocation -> store.get(invocation.getArgument(0, String.class)));
        org.mockito.Mockito.doAnswer(invocation -> {
            store.put(invocation.getArgument(0, String.class), invocation.getArgument(1, String.class));
            return null;
        }).when(ops).set(anyString(), anyString(), any(Duration.class));
        when(redis.delete(anyString())).thenAnswer(invocation -> store.remove(invocation.getArgument(0, String.class)) != null);

        PjbIdempotencyService service = new PjbIdempotencyService(redis, PjbIdempotencyPolicy.strict(), mock(AuditLedgerService.class));

        assertThat(service.acquire("abc")).isTrue();
        assertThat(service.status("abc")).isEqualTo("PROCESSING");

        service.complete("abc", 201, "application/json", "{\"ok\":true}", "/api/v1/protocolos/1");

        assertThat(service.status("abc")).isEqualTo("OK");
        Optional<PjbIdempotencyReplayPayload> replay = service.loadReplay("abc");
        assertThat(replay).isPresent();
        assertThat(replay.orElseThrow().status()).isEqualTo(201);
        assertThat(replay.orElseThrow().contentType()).isEqualTo("application/json");
        assertThat(replay.orElseThrow().body()).isEqualTo("{\"ok\":true}");
        assertThat(replay.orElseThrow().location()).isEqualTo("/api/v1/protocolos/1");
        assertThat(service.replayView("abc").replayed()).isTrue();
    }

    @Test
    void shouldReleaseKeyAndRemoveReplayWindow() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        Map<String, String> store = new ConcurrentHashMap<>();
        when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0, String.class);
            String value = invocation.getArgument(1, String.class);
            return store.putIfAbsent(key, value) == null;
        });
        when(ops.get(anyString())).thenAnswer(invocation -> store.get(invocation.getArgument(0, String.class)));
        org.mockito.Mockito.doAnswer(invocation -> {
            store.put(invocation.getArgument(0, String.class), invocation.getArgument(1, String.class));
            return null;
        }).when(ops).set(anyString(), anyString(), any(Duration.class));
        when(redis.delete(anyString())).thenAnswer(invocation -> store.remove(invocation.getArgument(0, String.class)) != null);

        PjbIdempotencyService service = new PjbIdempotencyService(redis, PjbIdempotencyPolicy.strict(), mock(AuditLedgerService.class));
        service.acquire("abc");
        service.complete("abc", 200, "application/json", "{}", null);

        service.release("abc");

        assertThat(service.status("abc")).isNull();
        assertThat(service.loadReplay("abc")).isEmpty();
        assertThat(service.window(new com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyWindowQuery("abc")).active()).isFalse();
    }
}
