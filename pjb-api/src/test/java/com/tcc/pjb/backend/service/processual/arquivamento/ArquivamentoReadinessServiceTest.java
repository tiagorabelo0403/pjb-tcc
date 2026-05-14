package com.tcc.pjb.backend.service.processual.arquivamento;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ArquivamentoReadinessServiceTest {

    private final ArquivamentoPendenciaChecker checker = newChecker();
    private final ArquivamentoReadinessService service = new ArquivamentoReadinessService(checker);

    private static ArquivamentoPendenciaChecker newChecker() {
        try {
            var c = ArquivamentoPendenciaChecker.class.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    @Test
    void avaliar_apto_quandoSemPendenciasBloqueantes() {
        var input = new ArquivamentoPendenciaChecker.ArquivamentoInput(
                true, false, false, false, true, true);
        var snapshot = service.avaliar(UUID.randomUUID(), input);
        assertThat(snapshot.apto()).isTrue();
        assertThat(snapshot.mensagem()).contains("apto para baixa");
    }

    @Test
    void avaliar_naoApto_quandoCustasPendentes() {
        var input = new ArquivamentoPendenciaChecker.ArquivamentoInput(
                false, false, false, false, true, true);
        var snapshot = service.avaliar(UUID.randomUUID(), input);
        assertThat(snapshot.apto()).isFalse();
        assertThat(snapshot.mensagem()).contains("bloqueante");
    }

    @Test
    void avaliar_naoApto_quandoExpedienteSemCiencia() {
        var input = new ArquivamentoPendenciaChecker.ArquivamentoInput(
                true, true, false, false, true, true);
        var snapshot = service.avaliar(UUID.randomUUID(), input);
        assertThat(snapshot.apto()).isFalse();
    }
}
