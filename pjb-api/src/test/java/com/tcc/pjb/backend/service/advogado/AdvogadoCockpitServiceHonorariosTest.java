package com.tcc.pjb.backend.service.advogado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.profile.operational.AdvogadoHonorariosCalculoRequest;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoHonorariosResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeGovernedProcessOperationService;
import com.tcc.pjb.backend.modules.custas.application.CustasApplicationService;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.processual.legitimidade.OabValidationService;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processual.honorarios.HonorariosSucumbenciaCalculatorService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AdvogadoCockpitServiceHonorariosTest {

    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
    private final HonorariosSucumbenciaCalculatorService calculatorService = new HonorariosSucumbenciaCalculatorService();

    private final AdvogadoCockpitService service = new AdvogadoCockpitService(
            mock(PerfilDashboardContextFactory.class),
            mock(PainelServiceCommons.class),
            processoRepository,
            mock(WorkItemRepository.class),
            authorizationService,
            mock(OfficeGovernedProcessOperationService.class),
            calculatorService,
            mock(CustasApplicationService.class),
            mock(OabValidationService.class),
            mock(com.tcc.pjb.backend.service.jurisprudencia.search.JurisprudenceContextualSearchService.class));

    @Test
    void calculaHonorariosDeTrabalhoComplexoAposAutorizarLeituraDoProcesso() {
        Processo processo = new Processo();
        processo.setId(42L);
        processo.setNumeroProcesso("0001234-56.2026.8.06.0001");
        when(processoRepository.findById(42L)).thenReturn(Optional.of(processo));

        AdvogadoHonorariosCalculoRequest request = new AdvogadoHonorariosCalculoRequest(
                new BigDecimal("100000"), false, false, true, null);

        AdvogadoHonorariosResponse response = service.calcularHonorarios(42L, request);

        assertThat(response.processoId()).isEqualTo(42L);
        assertThat(response.numeroProcesso()).isEqualTo("0001234-56.2026.8.06.0001");
        assertThat(response.percentualAplicado()).isEqualByComparingTo("0.20");
        assertThat(response.valorHonorarios()).isEqualByComparingTo("20000.00");
        verify(authorizationService).requireReadProcesso(processo);
    }

    @Test
    void negaCalculoQuandoAdvogadoNaoTemLeituraDoProcessoESemChamarCalculadora() {
        Processo processo = new Processo();
        processo.setId(7L);
        when(processoRepository.findById(7L)).thenReturn(Optional.of(processo));
        doThrow(new AccessDeniedPjbException("Acesso negado ao processo"))
                .when(authorizationService).requireReadProcesso(processo);

        AdvogadoHonorariosCalculoRequest request = new AdvogadoHonorariosCalculoRequest(
                new BigDecimal("50000"), false, false, false, null);

        assertThatThrownBy(() -> service.calcularHonorarios(7L, request))
                .isInstanceOf(AccessDeniedPjbException.class);
    }

    @Test
    void rejeitaProcessoInexistenteSemConsultarAutorizacaoOuCalculadora() {
        when(processoRepository.findById(999L)).thenReturn(Optional.empty());

        AdvogadoHonorariosCalculoRequest request = new AdvogadoHonorariosCalculoRequest(
                new BigDecimal("50000"), false, false, false, null);

        assertThatThrownBy(() -> service.calcularHonorarios(999L, request))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(authorizationService, never()).requireReadProcesso(any());
    }
}
