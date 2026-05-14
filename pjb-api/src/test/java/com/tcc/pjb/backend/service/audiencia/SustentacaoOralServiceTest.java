package com.tcc.pjb.backend.service.audiencia;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SustentacaoOralServiceTest {

    private final SustentacaoOralService service = new SustentacaoOralService();

    @Test
    void avaliar_habilitada_quandoTodosPressupostos() {
        var input = new SustentacaoOralService.SustentacaoInput(
                UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now().plusDays(1),
                true, true, true, false, false);
        var result = service.avaliar(input);
        assertThat(result.habilitada()).isTrue();
        assertThat(result.tempoMinutos()).isEqualTo(15);
    }

    @Test
    void avaliar_vedada_paraEmbargosDeclaracao() {
        var input = new SustentacaoOralService.SustentacaoInput(
                UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now().plusDays(1),
                true, true, true, true, false);
        var result = service.avaliar(input);
        assertThat(result.habilitada()).isFalse();
        assertThat(result.tempoMinutos()).isZero();
        assertThat(result.impedimentos()).anyMatch(i -> i.contains("Embargos de declaração"));
    }

    @Test
    void avaliar_impedida_semInscricao() {
        var input = new SustentacaoOralService.SustentacaoInput(
                UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now().plusDays(1),
                false, true, true, false, false);
        var result = service.avaliar(input);
        assertThat(result.habilitada()).isFalse();
        assertThat(result.impedimentos()).anyMatch(i -> i.contains("Inscrição"));
    }

    @Test
    void avaliar_impedida_semProcuracao() {
        var input = new SustentacaoOralService.SustentacaoInput(
                UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now().plusDays(1),
                true, false, true, false, false);
        var result = service.avaliar(input);
        assertThat(result.habilitada()).isFalse();
        assertThat(result.impedimentos()).anyMatch(i -> i.contains("Procuração"));
    }
}
