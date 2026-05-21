package com.tcc.pjb.backend.service.recuperacaojudicial;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.recuperacaojudicial.RecuperacaoJudicialReadinessService.RecuperacaoJudicialInput;
import com.tcc.pjb.backend.service.recuperacaojudicial.RecuperacaoJudicialReadinessService.RecuperacaoJudicialResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RecuperacaoJudicialReadinessServiceTest {

    private final RecuperacaoJudicialReadinessService service = new RecuperacaoJudicialReadinessService();

    @Test
    void deveRetornarSemPendenciaParaEmpresaLegitimada() {
        RecuperacaoJudicialInput input = new RecuperacaoJudicialInput(
                "12.345.678/0001-00",
                LocalDate.now().minusYears(5),
                false, null,
                false, false,
                new BigDecimal("500000.00"),
                true);

        RecuperacaoJudicialResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).isEmpty();
        assertThat(result.requisitosVerificados()).anyMatch(v -> v.contains("empresário") || v.contains("2 anos"));
    }

    @Test
    void deveAdicionarPendenciaQuandoNaoEmpresario() {
        RecuperacaoJudicialInput input = new RecuperacaoJudicialInput(
                "12.345.678/0001-00",
                LocalDate.now().minusYears(5),
                false, null,
                false, false,
                new BigDecimal("200000.00"),
                false);

        RecuperacaoJudicialResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("legitimidade") || p.contains("empresário"));
    }

    @Test
    void deveAdicionarPendenciaAtividadeMenorQue2Anos() {
        RecuperacaoJudicialInput input = new RecuperacaoJudicialInput(
                "12.345.678/0001-00",
                LocalDate.now().minusYears(1),
                false, null,
                false, false,
                new BigDecimal("100000.00"),
                true);

        RecuperacaoJudicialResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("2 anos") || p.contains("atividade"));
    }

    @Test
    void deveAdicionarPendenciaFalenciaAnteriorMenorQue5Anos() {
        RecuperacaoJudicialInput input = new RecuperacaoJudicialInput(
                "12.345.678/0001-00",
                LocalDate.now().minusYears(10),
                true, LocalDate.now().minusYears(3),
                false, false,
                new BigDecimal("300000.00"),
                true);

        RecuperacaoJudicialResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("falência") || p.contains("5 anos"));
    }

    @Test
    void deveAdicionarPendenciaRecuperacaoAnteriorMenorQue5Anos() {
        RecuperacaoJudicialInput input = new RecuperacaoJudicialInput(
                "12.345.678/0001-00",
                LocalDate.now().minusYears(8),
                false, null,
                false, true,
                new BigDecimal("400000.00"),
                true);

        RecuperacaoJudicialResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("recuperação judicial") || p.contains("5 anos"));
    }

    @Test
    void deveListarDocumentosNecessarios() {
        RecuperacaoJudicialInput input = new RecuperacaoJudicialInput(
                "12.345.678/0001-00",
                LocalDate.now().minusYears(5),
                false, null,
                false, false,
                new BigDecimal("200000.00"),
                true);

        RecuperacaoJudicialResult result = service.avaliar(input);

        assertThat(result.documentosNecessarios()).isNotEmpty();
        assertThat(result.documentosNecessarios()).anyMatch(d -> d.contains("contábeis") || d.contains("credores"));
    }
}
