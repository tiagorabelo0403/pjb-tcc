package com.tcc.pjb.backend.service.processual.nulidade;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class NulidadeProcessualRiskServiceTest {

    private final NulidadeProcessualRiskPolicy policy = newPolicy();
    private final NulidadeProcessualRiskService service = new NulidadeProcessualRiskService(policy);

    private static NulidadeProcessualRiskPolicy newPolicy() {
        try {
            var c = NulidadeProcessualRiskPolicy.class.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    @Test
    void diagnosticar_semRiscos_quandoTudoValido() {
        var input = new NulidadeProcessualRiskPolicy.NulidadeInput(
                true, true, true, true, true, true);
        var diagnostico = service.diagnosticar(UUID.randomUUID(), input);
        assertThat(diagnostico.riscos()).isEmpty();
        assertThat(diagnostico.bloqueioRecomendado()).isFalse();
        assertThat(diagnostico.mensagem()).contains("Nenhum risco");
    }

    @Test
    void diagnosticar_bloqueio_quandoIntimacaoInvalida() {
        var input = new NulidadeProcessualRiskPolicy.NulidadeInput(
                false, true, true, true, true, true);
        var diagnostico = service.diagnosticar(UUID.randomUUID(), input);
        assertThat(diagnostico.bloqueioRecomendado()).isTrue();
        assertThat(diagnostico.riscos()).anyMatch(r -> r.tipo().equals("INTIMACAO_INVALIDA"));
    }

    @Test
    void diagnosticar_bloqueio_quandoSemRepresentacao() {
        var input = new NulidadeProcessualRiskPolicy.NulidadeInput(
                true, false, true, true, true, true);
        var diagnostico = service.diagnosticar(UUID.randomUUID(), input);
        assertThat(diagnostico.bloqueioRecomendado()).isTrue();
        assertThat(diagnostico.riscos()).anyMatch(r -> r.tipo().equals("SEM_REPRESENTACAO"));
    }

    @Test
    void diagnosticar_riscosMultiplos_quandoVariasFalhas() {
        var input = new NulidadeProcessualRiskPolicy.NulidadeInput(
                false, false, false, false, false, false);
        var diagnostico = service.diagnosticar(UUID.randomUUID(), input);
        assertThat(diagnostico.riscos()).hasSize(6);
        assertThat(diagnostico.bloqueioRecomendado()).isTrue();
        assertThat(diagnostico.mensagem()).contains("6");
    }

    @Test
    void diagnosticar_riscosNaoCriticos_quandoApenasPrazoECompetencia() {
        var input = new NulidadeProcessualRiskPolicy.NulidadeInput(
                true, true, true, false, false, true);
        var diagnostico = service.diagnosticar(UUID.randomUUID(), input);
        assertThat(diagnostico.bloqueioRecomendado()).isFalse();
        assertThat(diagnostico.riscos()).hasSize(2);
    }
}
