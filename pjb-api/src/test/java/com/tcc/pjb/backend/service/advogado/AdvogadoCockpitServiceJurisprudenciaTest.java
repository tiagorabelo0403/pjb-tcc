package com.tcc.pjb.backend.service.advogado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.jurisprudencia.JurisprudenceContextualSearchResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeGovernedProcessOperationService;
import com.tcc.pjb.backend.modules.custas.application.CustasApplicationService;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.jurisprudencia.search.JurisprudenceContextualSearchService;
import com.tcc.pjb.backend.service.processual.honorarios.HonorariosSucumbenciaCalculatorService;
import com.tcc.pjb.backend.service.processual.legitimidade.OabValidationService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AdvogadoCockpitServiceJurisprudenciaTest {

    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
    private final JurisprudenceContextualSearchService jurisprudenceContextualSearchService = mock(JurisprudenceContextualSearchService.class);

    private final AdvogadoCockpitService service = new AdvogadoCockpitService(
            mock(PerfilDashboardContextFactory.class),
            mock(PainelServiceCommons.class),
            processoRepository,
            mock(WorkItemRepository.class),
            authorizationService,
            mock(OfficeGovernedProcessOperationService.class),
            new HonorariosSucumbenciaCalculatorService(),
            mock(CustasApplicationService.class),
            mock(OabValidationService.class),
            jurisprudenceContextualSearchService);

    @Test
    void buscaJurisprudenciaUsandoRamoERitoReaisDoProcessoAposAutorizarLeitura() {
        Processo processo = new Processo();
        processo.setId(60L);
        processo.setRamoDireito(RamoDireito.TRABALHISTA);
        processo.setRito(RitoProcessual.TRABALHISTA_ORDINARIO);
        when(processoRepository.findById(60L)).thenReturn(Optional.of(processo));
        JurisprudenceContextualSearchResponse resposta = new JurisprudenceContextualSearchResponse(
                "verbas rescisorias", "TRABALHISTA", "TRABALHISTA_ORDINARIO", List.of(), List.of(), List.of());
        when(jurisprudenceContextualSearchService.search("verbas rescisorias", RamoDireito.TRABALHISTA, RitoProcessual.TRABALHISTA_ORDINARIO, 10))
                .thenReturn(resposta);

        JurisprudenceContextualSearchResponse resultado = service.buscarJurisprudenciaDoProcesso(60L, "verbas rescisorias", 10);

        assertThat(resultado.ramo()).isEqualTo("TRABALHISTA");
        verify(authorizationService).requireReadProcesso(processo);
    }

    @Test
    void negaBuscaQuandoAdvogadoNaoTemLeituraDoProcessoESemChamarMotorDeBusca() {
        Processo processo = new Processo();
        processo.setId(61L);
        when(processoRepository.findById(61L)).thenReturn(Optional.of(processo));
        doThrow(new AccessDeniedPjbException("Acesso negado ao processo"))
                .when(authorizationService).requireReadProcesso(processo);

        assertThatThrownBy(() -> service.buscarJurisprudenciaDoProcesso(61L, "q", 10))
                .isInstanceOf(AccessDeniedPjbException.class);

        verify(jurisprudenceContextualSearchService, never()).search(any(), any(), any(), eq(10));
    }

    @Test
    void rejeitaProcessoInexistenteSemConsultarAutorizacaoOuMotorDeBusca() {
        when(processoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarJurisprudenciaDoProcesso(999L, "q", 10))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(authorizationService, never()).requireReadProcesso(any());
    }
}
