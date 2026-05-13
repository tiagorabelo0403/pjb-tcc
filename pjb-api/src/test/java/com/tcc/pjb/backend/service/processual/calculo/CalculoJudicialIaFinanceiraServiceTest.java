package com.tcc.pjb.backend.service.processual.calculo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialAssistenciaResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialIaFinanceiraCommandRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialResumoResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import com.tcc.pjb.backend.model.dto.processual.calculo.FazendaTributarioCalculoAvancadoRequest;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CalculoJudicialIaFinanceiraServiceTest {

    private final CalculoJudicialAssistenciaService assistenciaService = mock(CalculoJudicialAssistenciaService.class);
    private final CalculoJudicialFacadeService facadeService = mock(CalculoJudicialFacadeService.class);
    private final CalculoJudicialFrontendContractService contractService = new CalculoJudicialFrontendContractService(new CalculoJudicialTabelaOficialService(), TestEconomicReferenceSupport.economicReferenceService());
    private final CalculoJudicialIaFinanceiraService service = new CalculoJudicialIaFinanceiraService(assistenciaService, facadeService, contractService, TestEconomicReferenceSupport.economicReferenceService(), new ObjectMapper(), Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void deveExecutarCalculadoraRealQuandoNaoHouverPendenciasNemBloqueios() {
        LocalDate vencimento = LocalDate.of(2025, 11, 30);
        LocalDate dataCalculo = LocalDate.of(2026, 3, 29);
        FazendaTributarioCalculoAvancadoRequest request = new FazendaTributarioCalculoAvancadoRequest(
                "Teste",
                "0001",
                "União",
                "IRPF",
                CalculoJudicialSolicitantePerfil.ADVOGADO,
                "Advogado",
                "OAB/CE 1",
                new BigDecimal("15000.00"),
                vencimento,
                dataCalculo,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        CalculoJudicialAssistenciaResponse assistencia = new CalculoJudicialAssistenciaResponse(
                "FAZENDA_TRIBUTARIO",
                CalculoJudicialSolicitantePerfil.ADVOGADO,
                "Assistente",
                "ok",
                List.of(),
                List.of(),
                List.of(),
                List.of("ajuste"),
                List.of(),
                Map.of(),
                Map.of(),
                List.of("guardrail"),
                Instant.now()
        );
        CalculoJudicialResumoResponse resumo = new CalculoJudicialResumoResponse(
                "FAZENDA_TRIBUTARIO",
                "Resumo",
                "0001",
                CalculoJudicialSolicitantePerfil.ADVOGADO,
                "narrativa",
                "tecnica",
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("10.00"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                Instant.now()
        );
        when(assistenciaService.orientarFazenda(request, null)).thenReturn(assistencia);
        when(facadeService.calcularFazenda(any(FazendaTributarioCalculoAvancadoRequest.class), isNull())).thenReturn(resumo);

        var response = service.executarFazenda(request, null);

        assertThat(response.calculoExecutado()).isTrue();
        assertThat(response.status()).isEqualTo("READY");
        assertThat(response.resultado()).isEqualTo(resumo);
        assertThat(response.metadata()).containsEntry("sourceOfTruth", "CALCULADORA_REAL");
        assertThat(response.autopreenchimentoAplicado()).containsKey("percentualMultaMoraDiaria");
    }

    @Test
    void deveFicarPendenteQuandoFaltarDadoEssencial() {
        CalculoJudicialAssistenciaResponse assistencia = new CalculoJudicialAssistenciaResponse(
                "FAZENDA_TRIBUTARIO",
                CalculoJudicialSolicitantePerfil.CIDADAO,
                "Assistente",
                "ok",
                List.of(),
                List.of("Informar o valor principal."),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                Map.of(),
                List.of("guardrail"),
                Instant.now()
        );
        when(assistenciaService.orientarFazenda(null, null)).thenReturn(assistencia);

        var response = service.executarFazenda(null, null);

        assertThat(response.calculoExecutado()).isFalse();
        assertThat(response.status()).isEqualTo("PENDING_INPUT");
        assertThat(response.pendencias()).isNotEmpty();
    }

    @Test
    void deveExecutarPorRotaGenericaComSchemaValidationNoMetadata() {
        LocalDate vencimento = LocalDate.of(2025, 11, 30);
        LocalDate dataCalculo = LocalDate.of(2026, 3, 29);
        CalculoJudicialAssistenciaResponse assistencia = new CalculoJudicialAssistenciaResponse(
                "FAZENDA_TRIBUTARIO",
                CalculoJudicialSolicitantePerfil.ADVOGADO,
                "Assistente",
                "ok",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                Map.of(),
                List.of(),
                Instant.now()
        );
        CalculoJudicialResumoResponse resumo = new CalculoJudicialResumoResponse(
                "FAZENDA_TRIBUTARIO",
                "Resumo",
                "0001",
                CalculoJudicialSolicitantePerfil.ADVOGADO,
                "narrativa",
                "tecnica",
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("10.00"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                Instant.now()
        );
        when(assistenciaService.orientarFazenda(any(FazendaTributarioCalculoAvancadoRequest.class), isNull())).thenReturn(assistencia);
        when(facadeService.calcularFazenda(any(FazendaTributarioCalculoAvancadoRequest.class), isNull())).thenReturn(resumo);

        var response = service.executar(new CalculoJudicialIaFinanceiraCommandRequest(
                "fazenda-tributario",
                Map.of("principal", "15000.00", "dataVencimento", vencimento.toString(), "dataCalculo", dataCalculo.toString()),
                "Pode calcular para mim",
                "default_2026"
        ), null);

        assertThat(response.calculoExecutado()).isTrue();
        assertThat(response.metadata()).containsEntry("entryRoute", "/api/v1/processual/calculos/ia/financeira/executar");
        assertThat(response.metadata()).containsKey("schemaValidation");
        assertThat(response.metadata()).containsKey("routingConfidence");
    }

}
