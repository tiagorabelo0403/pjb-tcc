package com.tcc.pjb.backend.service.processual.paralisacao;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProcessoParalisacaoDiagnosisServiceTest {

    private final ProcessoParalisacaoDiagnosisService service =
            new ProcessoParalisacaoDiagnosisService();

    @Test
    void diagnosticar_expedienteSemCiencia_quandoFlagAtivo() {
        var input = new ProcessoParalisacaoDiagnosisService.ParalisacaoInput(
                UUID.randomUUID(), "0001234-12.2025.8.06.0001",
                Duration.ofDays(19), true, false, false, false, false, null);
        var resultado = service.diagnosticar(input);
        assertThat(resultado.causas()).anyMatch(c -> c instanceof ProcessoParalisacaoReason.ExpedienteSemCiencia);
        assertThat(resultado.resumo()).contains("19 dias");
    }

    @Test
    void diagnosticar_sobrestamento_quandoFlagAtivo() {
        var input = new ProcessoParalisacaoDiagnosisService.ParalisacaoInput(
                UUID.randomUUID(), "0001234-12.2025.8.06.0002",
                Duration.ofDays(30), false, false, false, false, true, "TEMA-1234");
        var resultado = service.diagnosticar(input);
        assertThat(resultado.causas())
                .anyMatch(c -> c instanceof ProcessoParalisacaoReason.SobrestamentoPorDependencia);
    }

    @Test
    void diagnosticar_causaNaoIdentificada_quandoSemFlags() {
        var input = new ProcessoParalisacaoDiagnosisService.ParalisacaoInput(
                UUID.randomUUID(), "0001234-12.2025.8.06.0003",
                Duration.ofDays(5), false, false, false, false, false, null);
        var resultado = service.diagnosticar(input);
        assertThat(resultado.causas()).hasSize(1);
        assertThat(resultado.causas().getFirst())
                .isInstanceOf(ProcessoParalisacaoReason.CausaNaoIdentificada.class);
    }

    @Test
    void diagnosticar_multiplasCausas_quandoVariosFlags() {
        var input = new ProcessoParalisacaoDiagnosisService.ParalisacaoInput(
                UUID.randomUUID(), "0001234-12.2025.8.06.0004",
                Duration.ofDays(45), true, true, true, false, false, null);
        var resultado = service.diagnosticar(input);
        assertThat(resultado.causas()).hasSize(3);
    }
}
