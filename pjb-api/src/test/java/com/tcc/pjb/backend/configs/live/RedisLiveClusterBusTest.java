package com.tcc.pjb.backend.configs.live;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class RedisLiveClusterBusTest {

    @Test
    void shouldBoundNamespacesAndHandlersPerNamespace() throws Exception {
        RedisLiveClusterBus bus = new RedisLiveClusterBus(mock(StringRedisTemplate.class), new ObjectMapper(), "pjb:live:", "node-a");

        for (int i = 0; i < 40; i++) {
            int slot = i;
            Consumer<LiveClusterEvent> handler = event -> { if (slot < 0) throw new IllegalStateException(); };
            bus.registerHandler("critical", handler);
        }
        for (int i = 0; i < 80; i++) {
            int slot = i;
            bus.registerHandler("ns-" + i, event -> { if (slot < 0) throw new IllegalStateException(); });
        }

        Field handlersField = RedisLiveClusterBus.class.getDeclaredField("handlers");
        handlersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> handlers = (Map<String, ?>) handlersField.get(bus);

        assertEquals(64, handlers.size());
        assertEquals(32, ((java.util.List<?>) handlers.get("critical")).size());
    }
}
