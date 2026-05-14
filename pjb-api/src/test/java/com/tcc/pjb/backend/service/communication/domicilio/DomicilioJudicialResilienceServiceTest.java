package com.tcc.pjb.backend.service.communication.domicilio;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DomicilioJudicialResilienceServiceTest {

    private final DomicilioJudicialFailureClassifier classifier = newClassifier();
    private final DomicilioJudicialRetryPlanner planner =
            new DomicilioJudicialRetryPlanner(classifier);
    private final DomicilioJudicialResilienceService service =
            new DomicilioJudicialResilienceService(classifier, planner);

    private static DomicilioJudicialFailureClassifier newClassifier() {
        try {
            var c = DomicilioJudicialFailureClassifier.class.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    @Test
    void registrarSucesso_enviado_semRetry() {
        var resultado = service.registrarSucesso(UUID.randomUUID());
        assertThat(resultado.enviado()).isTrue();
        assertThat(resultado.precisaRetry()).isFalse();
        assertThat(resultado.proximaTentativaEm()).isNull();
    }

    @Test
    void registrarFalha_comRetry_quandoServicoIndisponivel() {
        var resultado = service.registrarFalha(UUID.randomUUID(), 503, null, 0);
        assertThat(resultado.enviado()).isFalse();
        assertThat(resultado.precisaRetry()).isTrue();
        assertThat(resultado.proximaTentativaEm()).isNotNull();
    }

    @Test
    void registrarFalha_semRetry_quandoCredencialInvalida() {
        var resultado = service.registrarFalha(UUID.randomUUID(), 401, null, 1);
        assertThat(resultado.enviado()).isFalse();
        assertThat(resultado.precisaRetry()).isFalse();
    }

    @Test
    void registrarFalha_semRetry_quandoMaximoTentativas() {
        var resultado = service.registrarFalha(UUID.randomUUID(), 503, null, 5);
        assertThat(resultado.precisaRetry()).isFalse();
    }
}
