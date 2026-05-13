package com.tcc.pjb.backend.service.processual.calculo;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialAjuizamentoSignalRequest;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CalculoJudicialAjuizamentoSignalServiceTest {

    private final CalculoJudicialFrontendContractService contractService =
            new CalculoJudicialFrontendContractService(new CalculoJudicialTabelaOficialService(), TestEconomicReferenceSupport.economicReferenceService());

    private final PjbExecutionOrchestrator executionOrchestrator = org.mockito.Mockito.mock(PjbExecutionOrchestrator.class);

    private final CalculoJudicialAjuizamentoSignalService service =
            new CalculoJudicialAjuizamentoSignalService(new CalculoJudicialProfileResolverService(), contractService, TestEconomicReferenceSupport.economicReferenceService(), new CalculoJudicialAgentMeshService(executionOrchestrator, java.time.Duration.ofSeconds(5)));

    CalculoJudicialAjuizamentoSignalServiceTest() {
        org.mockito.Mockito.when(executionOrchestrator.supply(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> java.util.concurrent.CompletableFuture.completedFuture(((java.util.function.Supplier<?>) invocation.getArgument(1)).get()));
    }

    @Test
    void deveSugerirDominioTrabalhistaEAcionarMensagemTemporaria() {
        CalculoJudicialAjuizamentoSignalRequest request = new CalculoJudicialAjuizamentoSignalRequest(
                "Pretende horas extras, FGTS, multa do art. 477 e honorários sobre verbas rescisórias.",
                null,
                "TRABALHISTA",
                new BigDecimal("15000.00"),
                new BigDecimal("14000.00"),
                null,
                new BigDecimal("1500.00"),
                new BigDecimal("500.00"),
                4,
                Boolean.FALSE,
                Boolean.FALSE,
                java.util.Map.of()
        );

        var response = service.analisar(request, null);

        assertThat(response.requerCalculo()).isTrue();
        assertThat(response.dominioSugerido()).isEqualTo("TRABALHISTA_CLT");
        assertThat(response.mensagensTemporarias()).isNotEmpty();
        assertThat(response.routes()).containsEntry("liveAjuizamentoAssist", "/api/v1/processual/calculos/ia/financeira/sinalizar-ajuizamento");
    }
}
