package com.tcc.pjb.backend.service.communication.domicilio;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DomicilioJudicialRetryPlannerTest {

    private final DomicilioJudicialFailureClassifier classifier = newClassifier();
    private final DomicilioJudicialRetryPlanner planner =
            new DomicilioJudicialRetryPlanner(classifier);

    private static DomicilioJudicialFailureClassifier newClassifier() {
        try {
            var c = DomicilioJudicialFailureClassifier.class.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    @Test
    void planejar_deveRetentar_quandoServicoIndisponivel() {
        var plano = planner.planejar(0, 503, null);
        assertThat(plano.deveRetentar()).isTrue();
        assertThat(plano.proximoIntervalo()).isGreaterThan(Duration.ZERO);
    }

    @Test
    void planejar_naoRetentar_quandoMaximoAtingido() {
        var plano = planner.planejar(5, 503, null);
        assertThat(plano.deveRetentar()).isFalse();
        assertThat(plano.motivo()).contains("Máximo de tentativas");
    }

    @Test
    void planejar_naoRetentar_quandoCredencialInvalida() {
        var plano = planner.planejar(1, 401, null);
        assertThat(plano.deveRetentar()).isFalse();
    }

    @Test
    void planejar_backoffExponencial_aumentaComTentativas() {
        var plano1 = planner.planejar(0, 503, null);
        var plano2 = planner.planejar(1, 503, null);
        assertThat(plano2.proximoIntervalo()).isGreaterThan(plano1.proximoIntervalo());
    }
}
