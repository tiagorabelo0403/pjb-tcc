package com.tcc.pjb.backend.service.oficial_justica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioCartorioAckRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioChannelAckRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioConfirmationRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioReconciliationRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioRetryRequest;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.calendar.CalendarInstitutionalBridgeService;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorTopologyMeshService;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoIntelligenceSummaryService;
import com.tcc.pjb.backend.service.painel.shared.PainelCompositionPipelineService;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.InstitutionalMultimediaWorkspaceService;
import com.tcc.pjb.backend.service.profile.PerfilCapabilityMatrixService;
import com.tcc.pjb.backend.service.ui.branding.InstitutionalPanelBrandingService;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Prova que os métodos de ofício em {@link OficialJusticaPainelService} são delegates puros
 * para {@link OficialJusticaOficioDispatchService} -- extraído em F6 para tirar 6 dependências
 * exclusivas do bean raiz (30 -> 25 deps de construtor). Não testa a lógica de negócio de
 * emitir/responder/catálogo/ledger (isso é responsabilidade de OficialJusticaOficioDispatchService
 * e do próprio IT com Docker); testa só que a chamada certa acontece com os argumentos certos.
 */
class OficialJusticaPainelServiceOficioDelegationTest {

    private final OficialJusticaOficioDispatchService oficioDispatchService = mock(OficialJusticaOficioDispatchService.class);

    private final OficialJusticaPainelService service = new OficialJusticaPainelService(
            mock(PerfilDashboardContextFactory.class),
            mock(PainelServiceCommons.class),
            mock(ProcessoRepository.class),
            mock(WorkItemRepository.class),
            mock(PjbAuthorizationService.class),
            mock(PerfilCapabilityMatrixService.class),
            mock(PessoaLocalizacaoIntelligenceSummaryService.class),
            mock(InstitutionalActorTopologyMeshService.class),
            mock(InstitutionalActorRoutingService.class),
            mock(InstitutionalMultimediaWorkspaceService.class),
            mock(InstitutionalPanelBrandingService.class),
            oficioDispatchService,
            mock(OficialJusticaEnderecoTriageService.class),
            mock(OficialJusticaPortfolioProcessualService.class),
            mock(OficialJusticaWorkbenchService.class),
            mock(OficialJusticaAgendaOperacionalService.class),
            mock(OficialJusticaCalendarioOperacionalService.class),
            mock(OficialJusticaContextEnvelopeService.class),
            mock(OficialJusticaBalcaoVirtualService.class),
            mock(OficialJusticaNotificationCenterService.class),
            mock(OficialJusticaPanelEgressService.class),
            mock(CalendarInstitutionalBridgeService.class),
            mock(PainelSharedExperienceService.class),
            mock(PainelCompositionPipelineService.class),
            mock(OficialJusticaCommunicationFormalModelService.class)
    );

    @Test
    void catalogoOficiosDelegaSemArgumentos() {
        Map<String, Object> esperado = Map.of("catalogo", true);
        when(oficioDispatchService.catalogo()).thenReturn(esperado);

        assertThat(service.catalogoOficios()).isSameAs(esperado);
    }

    @Test
    void listarExecucoesOficiosDelegaComOMesmoLimit() {
        Map<String, Object> esperado = Map.of("execucoes", true);
        when(oficioDispatchService.listarExecucoes(7)).thenReturn(esperado);

        assertThat(service.listarExecucoesOficios(7)).isSameAs(esperado);
    }

    @Test
    void statusExecucaoOficioDelegaComOMesmoExecutionId() {
        Map<String, Object> esperado = Map.of("status", true);
        when(oficioDispatchService.statusExecucao("exec-1")).thenReturn(esperado);

        assertThat(service.statusExecucaoOficio("exec-1")).isSameAs(esperado);
    }

    @Test
    void confirmarEntregaOficioDelegaComExecutionIdERequest() {
        OficialJusticaOficioConfirmationRequest request = mock(OficialJusticaOficioConfirmationRequest.class);
        Map<String, Object> esperado = Map.of("entrega", true);
        when(oficioDispatchService.confirmarEntrega("exec-2", request)).thenReturn(esperado);

        assertThat(service.confirmarEntregaOficio("exec-2", request)).isSameAs(esperado);
    }

    @Test
    void confirmarCanalOficioDelegaComExecutionIdERequest() {
        OficialJusticaOficioChannelAckRequest request = mock(OficialJusticaOficioChannelAckRequest.class);
        Map<String, Object> esperado = Map.of("canal", true);
        when(oficioDispatchService.confirmarCanal("exec-3", request)).thenReturn(esperado);

        assertThat(service.confirmarCanalOficio("exec-3", request)).isSameAs(esperado);
    }

    @Test
    void ackCartorioOficioDelegaComExecutionIdERequest() {
        OficialJusticaOficioCartorioAckRequest request = mock(OficialJusticaOficioCartorioAckRequest.class);
        Map<String, Object> esperado = Map.of("cartorio", true);
        when(oficioDispatchService.ackCartorio("exec-4", request)).thenReturn(esperado);

        assertThat(service.ackCartorioOficio("exec-4", request)).isSameAs(esperado);
    }

    @Test
    void reconciliarOficioDelegaComExecutionIdERequest() {
        OficialJusticaOficioReconciliationRequest request = mock(OficialJusticaOficioReconciliationRequest.class);
        Map<String, Object> esperado = Map.of("reconciliar", true);
        when(oficioDispatchService.reconciliar("exec-5", request)).thenReturn(esperado);

        assertThat(service.reconciliarOficio("exec-5", request)).isSameAs(esperado);
    }

    @Test
    void malhaExternaOficioDelegaComOMesmoExecutionId() {
        Map<String, Object> esperado = Map.of("malha", true);
        when(oficioDispatchService.malhaExterna("exec-6")).thenReturn(esperado);

        assertThat(service.malhaExternaOficio("exec-6")).isSameAs(esperado);
    }

    @Test
    void retentarEntregaOficioDelegaComExecutionIdERequest() {
        OficialJusticaOficioRetryRequest request = mock(OficialJusticaOficioRetryRequest.class);
        Map<String, Object> esperado = Map.of("retentar", true);
        when(oficioDispatchService.retentar("exec-7", request)).thenReturn(esperado);

        assertThat(service.retentarEntregaOficio("exec-7", request)).isSameAs(esperado);
    }

    @Test
    void emitirOficioDelegaComProcessoIdERequest() {
        OficialJusticaOficioRequest request = mock(OficialJusticaOficioRequest.class);
        Map<String, Object> esperado = Map.of("emitido", true);
        when(oficioDispatchService.emitir(eq(42L), eq(request))).thenReturn(esperado);

        Map<String, Object> resultado = service.emitirOficio(42L, request);

        assertThat(resultado).isSameAs(esperado);
        verify(oficioDispatchService).emitir(42L, request);
    }

    @Test
    void responderOficioDelegaComProcessoIdERequest() {
        OficialJusticaOficioRequest request = mock(OficialJusticaOficioRequest.class);
        Map<String, Object> esperado = Map.of("respondido", true);
        when(oficioDispatchService.responder(eq(99L), eq(request))).thenReturn(esperado);

        Map<String, Object> resultado = service.responderOficio(99L, request);

        assertThat(resultado).isSameAs(esperado);
        verify(oficioDispatchService).responder(99L, request);
    }
}
