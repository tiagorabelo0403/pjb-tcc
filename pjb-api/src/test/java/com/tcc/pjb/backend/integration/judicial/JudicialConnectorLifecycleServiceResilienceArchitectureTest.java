package com.tcc.pjb.backend.integration.judicial;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class JudicialConnectorLifecycleServiceResilienceArchitectureTest {

    @Test
    void submitAndSynchronizeDeveEstarProtegidoPorResilience4j() throws NoSuchMethodException {
        Method method = JudicialConnectorLifecycleService.class.getDeclaredMethod(
                "submitAndSynchronize",
                com.tcc.pjb.backend.model.entity.Processo.class,
                com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport.class,
                com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionReport.class,
                boolean.class
        );

        assertThat(method.isAnnotationPresent(CircuitBreaker.class)).isTrue();
        assertThat(method.isAnnotationPresent(Retry.class)).isTrue();
        assertThat(method.isAnnotationPresent(Bulkhead.class)).isTrue();
    }
}
