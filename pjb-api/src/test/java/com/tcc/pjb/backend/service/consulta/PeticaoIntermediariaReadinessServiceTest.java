package com.tcc.pjb.backend.service.consulta;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PeticaoIntermediariaReadinessServiceTest {

    private final PeticaoIntermediariaReadinessService service = new PeticaoIntermediariaReadinessService();

    @Test
    void avaliar_apta_quandoTodosRequisitos() {
        var input = new PeticaoIntermediariaReadinessService.PeticaoInput(
                UUID.randomUUID(), "Impugnação ao Cumprimento de Sentença",
                true, true, true, true, true, true, true, true);
        var result = service.avaliar(input);
        assertThat(result.apta()).isTrue();
        assertThat(result.pendencias()).isEmpty();
    }

    @Test
    void avaliar_inapta_processoInativo() {
        var input = new PeticaoIntermediariaReadinessService.PeticaoInput(
                UUID.randomUUID(), "Petição Avulsa",
                false, true, true, true, true, true, true, false);
        var result = service.avaliar(input);
        assertThat(result.apta()).isFalse();
        assertThat(result.pendencias()).anyMatch(p -> p.contains("inativo"));
    }

    @Test
    void avaliar_inapta_semAdvogadoHabilitado() {
        var input = new PeticaoIntermediariaReadinessService.PeticaoInput(
                UUID.randomUUID(), "Impugnação",
                true, false, true, true, true, true, true, false);
        var result = service.avaliar(input);
        assertThat(result.apta()).isFalse();
        assertThat(result.pendencias()).anyMatch(p -> p.contains("Advogado não habilitado"));
    }

    @Test
    void avaliar_inapta_peticaoNaoAssinada() {
        var input = new PeticaoIntermediariaReadinessService.PeticaoInput(
                UUID.randomUUID(), "Embargos",
                true, true, true, false, true, true, true, false);
        var result = service.avaliar(input);
        assertThat(result.apta()).isFalse();
        assertThat(result.pendencias()).anyMatch(p -> p.contains("não assinada digitalmente"));
    }

    @Test
    void avaliar_alertaPrazo_quandoDecisaoAbertaSemVigilancia() {
        var input = new PeticaoIntermediariaReadinessService.PeticaoInput(
                UUID.randomUUID(), "Impugnação",
                true, true, true, true, true, true, false, true);
        var result = service.avaliar(input);
        assertThat(result.apta()).isFalse();
        assertThat(result.pendencias()).anyMatch(p -> p.contains("tempestividade"));
    }
}
