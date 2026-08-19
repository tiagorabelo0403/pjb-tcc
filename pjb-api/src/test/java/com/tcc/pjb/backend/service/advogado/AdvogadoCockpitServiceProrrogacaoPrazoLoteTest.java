package com.tcc.pjb.backend.service.advogado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeGovernedProcessOperationService;
import com.tcc.pjb.backend.modules.custas.application.CustasApplicationService;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.jurisprudencia.search.JurisprudenceContextualSearchService;
import com.tcc.pjb.backend.service.processual.honorarios.HonorariosSucumbenciaCalculatorService;
import com.tcc.pjb.backend.service.processual.legitimidade.OabValidationService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdvogadoCockpitServiceProrrogacaoPrazoLoteTest {

    private final OfficeGovernedProcessOperationService officeGovernedProcessOperationService =
            mock(OfficeGovernedProcessOperationService.class);

    private final AdvogadoCockpitService service = new AdvogadoCockpitService(
            mock(PerfilDashboardContextFactory.class),
            mock(PainelServiceCommons.class),
            mock(ProcessoRepository.class),
            mock(WorkItemRepository.class),
            mock(PjbAuthorizationService.class),
            officeGovernedProcessOperationService,
            new HonorariosSucumbenciaCalculatorService(),
            mock(CustasApplicationService.class),
            mock(OabValidationService.class),
            mock(JurisprudenceContextualSearchService.class));

    @Test
    void protocolizaPrazoEmLoteParaTodosOsProcessosQuandoNenhumFalha() {
        when(officeGovernedProcessOperationService.protocolizarPeticao(eq(10L), eq("PRORROGACAO_PRAZO"), anyString(), eq("audiência conflitante")))
                .thenReturn(Map.of("status", "PETIÇÃO_PROTOCOLIZADA"));
        when(officeGovernedProcessOperationService.protocolizarPeticao(eq(20L), eq("PRORROGACAO_PRAZO"), anyString(), eq("audiência conflitante")))
                .thenReturn(Map.of("status", "PETIÇÃO_PROTOCOLIZADA"));

        Map<String, Object> resultado = service.prorrogarPrazoEmLote(List.of(10L, 20L), "audiência conflitante");

        assertThat(resultado.get("total")).isEqualTo(2);
        assertThat(resultado.get("processados")).isEqualTo(2);
        assertThat(resultado.get("ids")).asList().containsExactly(10L, 20L);
        assertThat((List<?>) resultado.get("falhas")).isEmpty();
    }

    @Test
    void isolaFalhaDeUmProcessoSemInterromperOsDemaisDoLote() {
        when(officeGovernedProcessOperationService.protocolizarPeticao(eq(10L), eq("PRORROGACAO_PRAZO"), anyString(), anyString()))
                .thenReturn(Map.of("status", "PETIÇÃO_PROTOCOLIZADA"));
        doThrow(new AccessDeniedPjbException("Acesso negado ao processo"))
                .when(officeGovernedProcessOperationService).protocolizarPeticao(eq(30L), eq("PRORROGACAO_PRAZO"), anyString(), anyString());

        Map<String, Object> resultado = service.prorrogarPrazoEmLote(List.of(10L, 30L), "motivo");

        assertThat(resultado.get("total")).isEqualTo(2);
        assertThat(resultado.get("processados")).isEqualTo(1);
        assertThat(resultado.get("ids")).asList().containsExactly(10L);
        List<?> falhas = (List<?>) resultado.get("falhas");
        assertThat(falhas).hasSize(1);
        @SuppressWarnings("unchecked")
        Map<String, Object> falha = (Map<String, Object>) falhas.get(0);
        assertThat(falha.get("processoId")).isEqualTo(30L);

        verify(officeGovernedProcessOperationService).protocolizarPeticao(eq(10L), eq("PRORROGACAO_PRAZO"), anyString(), anyString());
        verify(officeGovernedProcessOperationService).protocolizarPeticao(eq(30L), eq("PRORROGACAO_PRAZO"), anyString(), anyString());
    }

    @Test
    void ignoraProcessosDuplicadosNoLote() {
        when(officeGovernedProcessOperationService.protocolizarPeticao(eq(10L), eq("PRORROGACAO_PRAZO"), anyString(), anyString()))
                .thenReturn(Map.of("status", "PETIÇÃO_PROTOCOLIZADA"));

        Map<String, Object> resultado = service.prorrogarPrazoEmLote(List.of(10L, 10L, 10L), "motivo");

        assertThat(resultado.get("processados")).isEqualTo(1);
        verify(officeGovernedProcessOperationService).protocolizarPeticao(eq(10L), eq("PRORROGACAO_PRAZO"), anyString(), anyString());
    }
}
