package com.tcc.pjb.backend.service.mandados;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MandadoReadinessServiceTest {

    private final MandadoReadinessService service = new MandadoReadinessService();

    @Test
    void avaliar_apto_quandoTudoConfirmado() {
        var input = new MandadoReadinessService.MandadoInput(
                UUID.randomUUID(), "0001234-12.2025.8.06.0001",
                true, true, true, true, true, true);
        var result = service.avaliar(input);
        assertThat(result.apto()).isTrue();
        assertThat(result.pendencias()).isEmpty();
    }

    @Test
    void avaliar_naoApto_quandoEnderecoNaoConfirmado() {
        var input = new MandadoReadinessService.MandadoInput(
                UUID.randomUUID(), "0001234-12.2025.8.06.0001",
                false, true, true, true, true, true);
        var result = service.avaliar(input);
        assertThat(result.apto()).isFalse();
        assertThat(result.pendencias()).anyMatch(p -> p.contains("Endereço"));
    }

    @Test
    void avaliar_naoApto_quandoSemAssinatura() {
        var input = new MandadoReadinessService.MandadoInput(
                UUID.randomUUID(), "0001234-12.2025.8.06.0001",
                true, true, true, true, false, true);
        var result = service.avaliar(input);
        assertThat(result.apto()).isFalse();
        assertThat(result.pendencias()).anyMatch(p -> p.contains("assinatura"));
    }

    @Test
    void avaliar_multiplasPendencias_quandoVariosProblemas() {
        var input = new MandadoReadinessService.MandadoInput(
                UUID.randomUUID(), "0001234-12.2025.8.06.0001",
                false, false, false, false, false, false);
        var result = service.avaliar(input);
        assertThat(result.pendencias()).hasSize(6);
        assertThat(result.mensagem()).contains("6");
    }
}
