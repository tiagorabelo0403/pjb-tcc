package com.tcc.pjb.backend.service.acaoconstitucional;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.acaoconstitucional.MandadoSegurancaReadinessService.MandadoSegurancaInput;
import com.tcc.pjb.backend.service.acaoconstitucional.MandadoSegurancaReadinessService.MandadoSegurancaResult;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MandadoSegurancaReadinessServiceTest {

    private final MandadoSegurancaReadinessService service = new MandadoSegurancaReadinessService();

    @Test
    void deveSinalizarSemPendenciasQuandoTodosRequisitosAtendidos() {
        MandadoSegurancaInput input = new MandadoSegurancaInput(
                "proc-001", LocalDate.now().minusDays(30),
                true, true, true, false, false, false);

        MandadoSegurancaResult result = service.avaliar(input);

        assertThat(result.prazoDecadencialVigente()).isTrue();
        assertThat(result.pendenciasIdentificadas()).isEmpty();
        assertThat(result.sinalizacao()).contains("Sem pendências");
    }

    @Test
    void deveAdicionarPendenciaQuandoPrazoDecadencialExpirado() {
        MandadoSegurancaInput input = new MandadoSegurancaInput(
                "proc-002", LocalDate.now().minusDays(130),
                true, true, true, false, false, false);

        MandadoSegurancaResult result = service.avaliar(input);

        assertThat(result.prazoDecadencialVigente()).isFalse();
        assertThat(result.diasRestantes()).isEqualTo(0);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("120 dias"));
    }

    @Test
    void deveAdicionarPendenciaParaLeiEmTese() {
        MandadoSegurancaInput input = new MandadoSegurancaInput(
                "proc-003", LocalDate.now().minusDays(10),
                true, true, true, true, false, false);

        MandadoSegurancaResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("Súmula 266"));
    }

    @Test
    void deveAdicionarPendenciaParaAtoComRecursoSuspensivo() {
        MandadoSegurancaInput input = new MandadoSegurancaInput(
                "proc-004", LocalDate.now().minusDays(15),
                true, true, true, false, true, false);

        MandadoSegurancaResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("Súmula 267"));
    }

    @Test
    void deveAdicionarPendenciaParaDireitoNaoLiquidoCerto() {
        MandadoSegurancaInput input = new MandadoSegurancaInput(
                "proc-005", LocalDate.now().minusDays(20),
                false, true, true, false, false, false);

        MandadoSegurancaResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("líquido e certo"));
    }

    @Test
    void deveCalcularDiasRestantesCorretos() {
        MandadoSegurancaInput input = new MandadoSegurancaInput(
                "proc-006", LocalDate.now().minusDays(60),
                true, true, true, false, false, false);

        MandadoSegurancaResult result = service.avaliar(input);

        assertThat(result.diasRestantes()).isEqualTo(60);
        assertThat(result.prazoDecadencialVigente()).isTrue();
    }

    @Test
    void deveAdicionarVerificadoParaRequisitosAtendidos() {
        MandadoSegurancaInput input = new MandadoSegurancaInput(
                "proc-007", LocalDate.now().minusDays(5),
                true, true, true, false, false, false);

        MandadoSegurancaResult result = service.avaliar(input);

        assertThat(result.requisitosVerificados()).anyMatch(v -> v.contains("líquido e certo"));
        assertThat(result.requisitosVerificados()).anyMatch(v -> v.contains("pré-constituída"));
    }
}
