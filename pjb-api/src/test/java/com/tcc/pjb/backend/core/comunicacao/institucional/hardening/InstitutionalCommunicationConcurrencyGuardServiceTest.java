package com.tcc.pjb.backend.core.comunicacao.institucional.hardening;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.comunicacao.institucional.hardening.application.InstitutionalCommunicationConcurrencyGuardService;
import com.tcc.pjb.backend.core.comunicacao.institucional.hardening.domain.InstitutionalConcurrentOperationException;
import com.tcc.pjb.backend.platform.cluster.PjbLocalClusterLockService;

class InstitutionalCommunicationConcurrencyGuardServiceTest {

    @Test
    void shouldExecuteProtectedOperationWhenLockIsAvailable() {
        PjbLocalClusterLockService lockService = new PjbLocalClusterLockService("", new SimpleMeterRegistry());
        InstitutionalCommunicationConcurrencyGuardService service = new InstitutionalCommunicationConcurrencyGuardService(lockService);

        String result = service.execute("receber", "exp-1", () -> "ok");

        assertEquals("ok", result);
    }

    @Test
    void shouldRejectConcurrentOperationWhenResourceIsLocked() {
        PjbLocalClusterLockService lockService = new PjbLocalClusterLockService("", new SimpleMeterRegistry());
        InstitutionalCommunicationConcurrencyGuardService service = new InstitutionalCommunicationConcurrencyGuardService(lockService);
        try (var ignored = lockService.tryAcquire("institutional:receber:exp-1", Duration.ofSeconds(60)).orElseThrow()) {
            assertThrows(InstitutionalConcurrentOperationException.class, () -> service.execute("receber", "exp-1", () -> "should-fail"));
        }
    }
}
