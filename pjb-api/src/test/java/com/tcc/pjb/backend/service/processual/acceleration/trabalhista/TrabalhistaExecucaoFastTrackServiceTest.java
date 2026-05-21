package com.tcc.pjb.backend.service.processual.acceleration.trabalhista;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.processual.acceleration.trabalhista.TrabalhistaExecucaoFastTrackService.ExecucaoInput;
import com.tcc.pjb.backend.service.processual.acceleration.trabalhista.TrabalhistaExecucaoFastTrackService.FastTrackElegibilidade;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrabalhistaExecucaoFastTrackServiceTest {

    private final TrabalhistaExecucaoFastTrackService service =
            new TrabalhistaExecucaoFastTrackService();

    @Test
    void deveRetornarElegivelQuandoTodosRequisitosAtendidos() {
        UUID processoId = UUID.randomUUID();
        ExecucaoInput input = new ExecucaoInput(
                processoId, true, true, true, true, false);

        FastTrackElegibilidade result = service.avaliar(input);

        assertThat(result.elegivel()).isTrue();
        assertThat(result.pendencias()).isEmpty();
        assertThat(result.processoId()).isEqualTo(processoId);
    }

    @Test
    void deveAdicionarPendenciaQuandoTituloNaoLiquido() {
        ExecucaoInput input = new ExecucaoInput(
                UUID.randomUUID(), false, true, true, true, false);

        FastTrackElegibilidade result = service.avaliar(input);

        assertThat(result.elegivel()).isFalse();
        assertThat(result.pendencias()).anyMatch(p -> p.contains("líquido"));
    }

    @Test
    void deveAdicionarPendenciaQuandoDevedorNaoLocalizavel() {
        ExecucaoInput input = new ExecucaoInput(
                UUID.randomUUID(), true, false, true, true, false);

        FastTrackElegibilidade result = service.avaliar(input);

        assertThat(result.pendencias()).anyMatch(p -> p.contains("BACENJUD"));
    }

    @Test
    void deveAdicionarEtapaGarantiaDeposito() {
        ExecucaoInput input = new ExecucaoInput(
                UUID.randomUUID(), true, true, true, true, true);

        FastTrackElegibilidade result = service.avaliar(input);

        assertThat(result.etapasDesbloqueadas()).anyMatch(e -> e.contains("embargos"));
    }

    @Test
    void deveAdicionarPendenciaCalculosNaoHomologados() {
        ExecucaoInput input = new ExecucaoInput(
                UUID.randomUUID(), true, true, true, false, false);

        FastTrackElegibilidade result = service.avaliar(input);

        assertThat(result.pendencias()).anyMatch(p -> p.contains("Cálculos"));
    }

    @Test
    void deveNaoSerElegivelQuandoHaPendencias() {
        ExecucaoInput input = new ExecucaoInput(
                UUID.randomUUID(), false, false, false, false, false);

        FastTrackElegibilidade result = service.avaliar(input);

        assertThat(result.elegivel()).isFalse();
        assertThat(result.pendencias()).isNotEmpty();
    }
}
