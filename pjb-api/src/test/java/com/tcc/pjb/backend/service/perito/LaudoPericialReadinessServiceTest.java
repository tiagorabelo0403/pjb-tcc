package com.tcc.pjb.backend.service.perito;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.perito.LaudoPericialReadinessService.LaudoInput;
import com.tcc.pjb.backend.service.perito.LaudoPericialReadinessService.LaudoReadiness;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LaudoPericialReadinessServiceTest {

    private final LaudoPericialReadinessService service = new LaudoPericialReadinessService();

    @Test
    void deveRetornarAptoQuandoTodosRequisitosAtendidos() {
        LaudoInput input = new LaudoInput(
                UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.now().plusDays(10),
                true, true, true, true, true);

        LaudoReadiness result = service.avaliar(input);

        assertThat(result.apto()).isTrue();
        assertThat(result.pendencias()).isEmpty();
        assertThat(result.proximaProvidencia()).contains("ordem");
    }

    @Test
    void deveAdicionarPendenciaLaudoNaoEntregue() {
        LaudoInput input = new LaudoInput(
                UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.now().plusDays(5),
                false, false, true, false, false);

        LaudoReadiness result = service.avaliar(input);

        assertThat(result.apto()).isFalse();
        assertThat(result.pendencias()).anyMatch(p -> p.contains("não entregue"));
    }

    @Test
    void deveDetectarAtrasoQuandoPrazoVencido() {
        LaudoInput input = new LaudoInput(
                UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.now().minusDays(5),
                false, false, true, false, false);

        LaudoReadiness result = service.avaliar(input);

        assertThat(result.atrasado()).isTrue();
        assertThat(result.diasAtraso()).isEqualTo(5);
    }

    @Test
    void deveAdicionarPendenciaHonorariosNaoDepositados() {
        LaudoInput input = new LaudoInput(
                UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.now().plusDays(10),
                true, true, false, true, true);

        LaudoReadiness result = service.avaliar(input);

        assertThat(result.pendencias()).anyMatch(p -> p.contains("Honorários"));
    }

    @Test
    void deveAdicionarPendenciaQuesitosNaoRespondidos() {
        LaudoInput input = new LaudoInput(
                UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.now().plusDays(10),
                true, true, true, false, true);

        LaudoReadiness result = service.avaliar(input);

        assertThat(result.pendencias()).anyMatch(p -> p.contains("Quesitos"));
    }

    @Test
    void deveAdicionarPendenciaPartesNaoIntimadas() {
        LaudoInput input = new LaudoInput(
                UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.now().plusDays(10),
                true, true, true, true, false);

        LaudoReadiness result = service.avaliar(input);

        assertThat(result.pendencias()).anyMatch(p -> p.contains("intimadas"));
    }

    @Test
    void deveRetornarNaoAtrasadoQuandoPrazoFuturo() {
        LaudoInput input = new LaudoInput(
                UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.now().plusDays(15),
                false, false, true, false, false);

        LaudoReadiness result = service.avaliar(input);

        assertThat(result.atrasado()).isFalse();
        assertThat(result.diasAtraso()).isEqualTo(0);
    }
}
