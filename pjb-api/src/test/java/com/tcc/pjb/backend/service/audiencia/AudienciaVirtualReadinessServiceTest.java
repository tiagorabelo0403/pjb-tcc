package com.tcc.pjb.backend.service.audiencia;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AudienciaVirtualReadinessServiceTest {

    private final AudienciaVirtualReadinessService service = new AudienciaVirtualReadinessService();

    @Test
    void avaliar_apto_quandoTodosRequisitosAtendidos() {
        var input = inputCompleto(AudienciaTipo.CONCILIACAO, false);
        var result = service.avaliar(input);
        assertThat(result.apto()).isTrue();
        assertThat(result.pendencias()).isEmpty();
    }

    @Test
    void avaliar_inapto_quandoTipoNaoAdmiteVirtual() {
        var input = new AudienciaVirtualReadinessService.VirtualReadinessInput(
                UUID.randomUUID(), AudienciaTipo.CUSTODIAL,
                true, true, true, true, true, false, true);
        var result = service.avaliar(input);
        assertThat(result.apto()).isFalse();
        assertThat(result.pendencias()).anyMatch(p -> p.contains("não admite audiência virtual"));
    }

    @Test
    void avaliar_pendencia_quandoPlataformaNaoHabilitada() {
        var input = new AudienciaVirtualReadinessService.VirtualReadinessInput(
                UUID.randomUUID(), AudienciaTipo.CONCILIACAO,
                false, true, true, true, true, false, true);
        var result = service.avaliar(input);
        assertThat(result.apto()).isFalse();
        assertThat(result.pendencias()).anyMatch(p -> p.contains("não habilitada"));
    }

    @Test
    void avaliar_pendencia_quandoSemAcordoParticipantes() {
        var input = new AudienciaVirtualReadinessService.VirtualReadinessInput(
                UUID.randomUUID(), AudienciaTipo.MEDIACAO,
                true, true, true, true, true, false, false);
        var result = service.avaliar(input);
        assertThat(result.apto()).isFalse();
        assertThat(result.pendencias()).anyMatch(p -> p.contains("concordância"));
    }

    @Test
    void avaliar_alertaMenorEnvolvido() {
        var input = new AudienciaVirtualReadinessService.VirtualReadinessInput(
                UUID.randomUUID(), AudienciaTipo.CONCILIACAO,
                true, true, true, true, true, true, true);
        var result = service.avaliar(input);
        assertThat(result.pendencias()).anyMatch(p -> p.contains("Menor envolvido"));
    }

    private AudienciaVirtualReadinessService.VirtualReadinessInput inputCompleto(
            AudienciaTipo tipo, boolean menor) {
        return new AudienciaVirtualReadinessService.VirtualReadinessInput(
                UUID.randomUUID(), tipo, true, true, true, true, true, menor, true);
    }
}
