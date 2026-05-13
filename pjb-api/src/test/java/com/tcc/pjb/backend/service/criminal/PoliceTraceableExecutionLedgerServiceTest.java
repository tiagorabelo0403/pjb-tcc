package com.tcc.pjb.backend.service.criminal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PoliceTraceableExecutionLedgerServiceTest {

    @Test
    void shouldTrimExecutionLedgerWhenOverflowHappens() throws Exception {
        PoliceTraceableExecutionLedgerService service = new PoliceTraceableExecutionLedgerService(mock(AuditLedgerService.class));
        @SuppressWarnings("unchecked")
        Map<String, Object> states = (Map<String, Object>) field(service, "states").get(service);
        Instant now = Instant.now();

        for (int i = 0; i < 20_050; i++) {
            String status = i < 60 ? "CONFIRMADO_PELO_PARCEIRO" : "AGUARDANDO_CONFIRMACAO_EXTERNA";
            Instant updatedAt = now.minusSeconds(10_000L).plusMillis(i);
            states.put("exec-" + i, newState("exec-" + i, status, updatedAt));
        }

        Method cleanup = PoliceTraceableExecutionLedgerService.class.getDeclaredMethod("cleanupIfRequired", Instant.class, boolean.class);
        cleanup.setAccessible(true);
        cleanup.invoke(service, now, true);

        assertThat(states).hasSize(20_000);
        assertThat(states).doesNotContainKeys("exec-0", "exec-1", "exec-2", "exec-3", "exec-4", "exec-5", "exec-6", "exec-7", "exec-8", "exec-9",
                "exec-10", "exec-11", "exec-12", "exec-13", "exec-14", "exec-15", "exec-16", "exec-17", "exec-18", "exec-19",
                "exec-20", "exec-21", "exec-22", "exec-23", "exec-24", "exec-25", "exec-26", "exec-27", "exec-28", "exec-29",
                "exec-30", "exec-31", "exec-32", "exec-33", "exec-34", "exec-35", "exec-36", "exec-37", "exec-38", "exec-39",
                "exec-40", "exec-41", "exec-42", "exec-43", "exec-44", "exec-45", "exec-46", "exec-47", "exec-48", "exec-49");
        assertThat(states).containsKeys("exec-50", "exec-60", "exec-20049");
    }

    private Object newState(String executionId, String status, Instant updatedAt) throws Exception {
        Class<?> stateClass = Class.forName("com.tcc.pjb.backend.service.criminal.PoliceTraceableExecutionLedgerService$TraceableExecutionState");
        Constructor<?> constructor = stateClass.getDeclaredConstructor(
                String.class,
                String.class,
                String.class,
                Long.class,
                Long.class,
                String.class,
                String.class,
                boolean.class,
                String.class,
                String.class,
                int.class,
                int.class,
                Instant.class,
                Instant.class,
                Instant.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                List.class,
                List.class,
                Map.class,
                Map.class,
                List.class
        );
        constructor.setAccessible(true);
        return constructor.newInstance(
                executionId,
                "POLICE_LANE",
                "OPERACAO_TESTE",
                1L,
                2L,
                "PJB_NATIVE",
                "OPERACAO_TESTE",
                true,
                "HIGH",
                status,
                0,
                7,
                updatedAt.minusSeconds(60),
                updatedAt,
                null,
                "queue.confirmation",
                "queue.error",
                "queue.reconciliation",
                "idem-" + executionId,
                "route-" + executionId,
                "audit-" + executionId,
                List.of("PJB_NATIVE"),
                List.of("STEP"),
                Map.of("plan", executionId),
                Map.of(),
                List.of(Map.of("status", status, "occurredAt", updatedAt.toString()))
        );
    }

    private Field field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
