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
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoCustaItemResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeGovernedProcessOperationService;
import com.tcc.pjb.backend.modules.custas.application.CustasApplicationService;
import com.tcc.pjb.backend.modules.custas.domain.CustaConsultaResult;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processual.honorarios.HonorariosSucumbenciaCalculatorService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AdvogadoCockpitServiceCustasTest {

    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
    private final CustasApplicationService custasApplicationService = mock(CustasApplicationService.class);

    private final AdvogadoCockpitService service = new AdvogadoCockpitService(
            mock(PerfilDashboardContextFactory.class),
            mock(PainelServiceCommons.class),
            processoRepository,
            mock(WorkItemRepository.class),
            authorizationService,
            mock(OfficeGovernedProcessOperationService.class),
            new HonorariosSucumbenciaCalculatorService(),
            custasApplicationService);

    @Test
    void listaCustasDoProcessoAposAutorizarLeituraDoProcesso() {
        Processo processo = new Processo();
        processo.setId(30L);
        when(processoRepository.findById(30L)).thenReturn(Optional.of(processo));
        when(custasApplicationService.listarPorProcesso(30L)).thenReturn(List.of(
                new CustaConsultaResult(1L, "TAXA_JUDICIARIA", new BigDecimal("150.00"), "PENDENTE", LocalDate.now().plusDays(30), null, null)
        ));

        List<AdvogadoCustaItemResponse> resultado = service.listarCustas(30L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).custaId()).isEqualTo(1L);
        assertThat(resultado.get(0).tipo()).isEqualTo("TAXA_JUDICIARIA");
        assertThat(resultado.get(0).status()).isEqualTo("PENDENTE");
        verify(authorizationService).requireReadProcesso(processo);
    }

    @Test
    void negaListagemQuandoAdvogadoNaoTemLeituraDoProcessoESemConsultarCustas() {
        Processo processo = new Processo();
        processo.setId(31L);
        when(processoRepository.findById(31L)).thenReturn(Optional.of(processo));
        doThrow(new AccessDeniedPjbException("Acesso negado ao processo"))
                .when(authorizationService).requireReadProcesso(processo);

        assertThatThrownBy(() -> service.listarCustas(31L))
                .isInstanceOf(AccessDeniedPjbException.class);

        verify(custasApplicationService, never()).listarPorProcesso(any());
    }

    @Test
    void rejeitaProcessoInexistenteSemConsultarAutorizacaoOuCustas() {
        when(processoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listarCustas(999L))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(authorizationService, never()).requireReadProcesso(any());
        verify(custasApplicationService, never()).listarPorProcesso(any());
    }
}
