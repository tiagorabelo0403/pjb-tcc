package com.tcc.pjb.backend.service.processual.acceleration.penal;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.processual.acceleration.penal.PenalAudienciaInstrucaoReadinessService.ReadinessInput;
import com.tcc.pjb.backend.service.processual.acceleration.penal.PenalAudienciaInstrucaoReadinessService.ReadinessSnapshot;
import org.junit.jupiter.api.Test;

class PenalAudienciaInstrucaoReadinessServiceTest {

    private final PenalAudienciaInstrucaoReadinessService service =
            new PenalAudienciaInstrucaoReadinessService();

    @Test
    void deveRetornarAptoQuandoTodosRequisitosAtendidos() {
        ReadinessInput input = new ReadinessInput(
                true, true, true, true, true, true, true);

        ReadinessSnapshot result = service.avaliar(input);

        assertThat(result.apto()).isTrue();
        assertThat(result.pendencias()).isEmpty();
        assertThat(result.mensagem()).contains("apto");
    }

    @Test
    void deveAdicionarPendenciaQuandoAcusacaoNaoRecebida() {
        ReadinessInput input = new ReadinessInput(
                false, true, true, true, true, true, true);

        ReadinessSnapshot result = service.avaliar(input);

        assertThat(result.apto()).isFalse();
        assertThat(result.pendencias()).anyMatch(p -> p.contains("Acusação"));
    }

    @Test
    void deveAdicionarPendenciaQuandoReuNaoCitado() {
        ReadinessInput input = new ReadinessInput(
                true, false, true, true, true, true, true);

        ReadinessSnapshot result = service.avaliar(input);

        assertThat(result.pendencias()).anyMatch(p -> p.contains("citado"));
    }

    @Test
    void deveAdicionarPendenciaIntimacaoQuandoTestemunhasArrolaradas() {
        ReadinessInput input = new ReadinessInput(
                true, true, true, true, true, false, true);

        ReadinessSnapshot result = service.avaliar(input);

        assertThat(result.pendencias()).anyMatch(p -> p.contains("Intimação de testemunhas"));
    }

    @Test
    void deveNaoExigirIntimacaoQuandoSemTestemunhas() {
        ReadinessInput input = new ReadinessInput(
                true, true, true, true, false, false, true);

        ReadinessSnapshot result = service.avaliar(input);

        assertThat(result.pendencias()).noneMatch(p -> p.contains("testemunhas"));
    }

    @Test
    void deveAdicionarPendenciaReuPresoSemAutorizacao() {
        ReadinessInput input = new ReadinessInput(
                true, true, true, true, false, false, false);

        ReadinessSnapshot result = service.avaliar(input);

        assertThat(result.pendencias()).anyMatch(p -> p.contains("presídio"));
    }

    @Test
    void deveMensagemContarPendencias() {
        ReadinessInput input = new ReadinessInput(
                false, false, false, false, false, false, false);

        ReadinessSnapshot result = service.avaliar(input);

        assertThat(result.mensagem()).contains("pendência");
        assertThat(result.pendencias().size()).isGreaterThan(1);
    }
}
