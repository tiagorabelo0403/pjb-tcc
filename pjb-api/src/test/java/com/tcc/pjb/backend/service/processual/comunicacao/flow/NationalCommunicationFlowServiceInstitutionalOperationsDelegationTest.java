package com.tcc.pjb.backend.service.processual.comunicacao.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.canonico.AtoCanonicoProcessualResolver;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.application.InstitutionalDeliveryQueueApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalDocumentSecurityGateApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.application.InstitutionalInboxApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.routing.MotorRoteamentoComunicacaoInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalFlowAnalyticsApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalWorkflowApplicationService;
import com.tcc.pjb.backend.core.comunicacao.judicial.CitacaoIntimacaoEngine;
import com.tcc.pjb.backend.core.comunicacao.processual.destinatario.application.DestinatarioProcessualResolverApplicationService;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalAccessCheckRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalAccessCheckResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalMembershipResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalActionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalDeadLetterResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalDeliveryProofResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalExternalDispatchResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalFulfillRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalGateStateResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalReceiveRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalRedistributeRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalReprocessDeliveryRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalScienceRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalDeliveryQueueItemResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalInboxItemResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalObservabilityDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalTimelineEventResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalHardeningReportResponse;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.StatusComunicacaoInstitucional;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationsFacade;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NationalCommunicationFlowServiceInstitutionalOperationsDelegationTest {

    private final NationalCommunicationInstitutionalOperationsFacade institutionalOperationsFacade = mock(NationalCommunicationInstitutionalOperationsFacade.class);

    private final NationalCommunicationFlowService service = new NationalCommunicationFlowService(
            mock(CitacaoIntimacaoEngine.class),
            mock(ProcessoRepository.class),
            mock(WorkItemRepository.class),
            mock(ProcessoLifecycleMachine.class),
            mock(CurrentUserService.class),
            mock(PjbAuthorizationService.class),
            mock(AuditLedgerService.class),
            mock(CatalogoInstitucionalUnificadoService.class),
            mock(AtoCanonicoProcessualResolver.class),
            mock(MotorRoteamentoComunicacaoInstitucional.class),
            mock(InstitutionalInboxApplicationService.class),
            mock(InstitutionalDeliveryQueueApplicationService.class),
            mock(InstitutionalWorkflowApplicationService.class),
            mock(InstitutionalFlowAnalyticsApplicationService.class),
            mock(DestinatarioProcessualResolverApplicationService.class),
            mock(InstitutionalDocumentSecurityGateApplicationService.class),
            institutionalOperationsFacade
    );

    @Test
    void minhasCaixasInstitucionaisDelegaComOsMesmosArgumentos() {
        List<NationalCommunicationInstitutionalMembershipResponse> esperado = new ArrayList<>(List.of(mock(NationalCommunicationInstitutionalMembershipResponse.class)));
        when(institutionalOperationsFacade.minhasCaixasInstitucionais(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO, "CE", "Fortaleza")).thenReturn(esperado);

        assertThat(service.minhasCaixasInstitucionais(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO, "CE", "Fortaleza")).isSameAs(esperado);
    }

    @Test
    void autorizarCaixaInstitucionalDelega() {
        var request = mock(NationalCommunicationInstitutionalAccessCheckRequest.class);
        NationalCommunicationInstitutionalAccessCheckResponse esperado = mock(NationalCommunicationInstitutionalAccessCheckResponse.class);
        when(institutionalOperationsFacade.autorizarCaixaInstitucional(request)).thenReturn(esperado);

        assertThat(service.autorizarCaixaInstitucional(request)).isSameAs(esperado);
    }

    @Test
    void listarInboxInstitucionalDelegaComOsMesmosArgumentos() {
        List<NationalCommunicationInstitutionalInboxItemResponse> esperado = new ArrayList<>(List.of(mock(NationalCommunicationInstitutionalInboxItemResponse.class)));
        when(institutionalOperationsFacade.listarInboxInstitucional(StatusComunicacaoInstitucional.RECEBIDA, 10L)).thenReturn(esperado);

        assertThat(service.listarInboxInstitucional(StatusComunicacaoInstitucional.RECEBIDA, 10L)).isSameAs(esperado);
    }

    @Test
    void receberInboxInstitucionalDelega() {
        var request = mock(NationalCommunicationInstitutionalReceiveRequest.class);
        NationalCommunicationInstitutionalActionResponse esperado = mock(NationalCommunicationInstitutionalActionResponse.class);
        when(institutionalOperationsFacade.receberInboxInstitucional(request)).thenReturn(esperado);

        assertThat(service.receberInboxInstitucional(request)).isSameAs(esperado);
    }

    @Test
    void redistribuirInboxInstitucionalDelega() {
        var request = mock(NationalCommunicationInstitutionalRedistributeRequest.class);
        NationalCommunicationInstitutionalActionResponse esperado = mock(NationalCommunicationInstitutionalActionResponse.class);
        when(institutionalOperationsFacade.redistribuirInboxInstitucional(request)).thenReturn(esperado);

        assertThat(service.redistribuirInboxInstitucional(request)).isSameAs(esperado);
    }

    @Test
    void certificarCienciaInstitucionalDelega() {
        var request = mock(NationalCommunicationInstitutionalScienceRequest.class);
        NationalCommunicationInstitutionalActionResponse esperado = mock(NationalCommunicationInstitutionalActionResponse.class);
        when(institutionalOperationsFacade.certificarCienciaInstitucional(request)).thenReturn(esperado);

        assertThat(service.certificarCienciaInstitucional(request)).isSameAs(esperado);
    }

    @Test
    void cumprirInboxInstitucionalDelega() {
        var request = mock(NationalCommunicationInstitutionalFulfillRequest.class);
        NationalCommunicationInstitutionalActionResponse esperado = mock(NationalCommunicationInstitutionalActionResponse.class);
        when(institutionalOperationsFacade.cumprirInboxInstitucional(request)).thenReturn(esperado);

        assertThat(service.cumprirInboxInstitucional(request)).isSameAs(esperado);
    }

    @Test
    void timelineInstitucionalDelegaComOMesmoExpedicaoUuid() {
        List<NationalCommunicationInstitutionalTimelineEventResponse> esperado = new ArrayList<>(List.of(mock(NationalCommunicationInstitutionalTimelineEventResponse.class)));
        when(institutionalOperationsFacade.timelineInstitucional("uuid-1")).thenReturn(esperado);

        assertThat(service.timelineInstitucional("uuid-1")).isSameAs(esperado);
    }

    @Test
    void provasInstitucionaisDelegaComOMesmoExpedicaoUuid() {
        List<NationalCommunicationInstitutionalDeliveryProofResponse> esperado = new ArrayList<>(List.of(mock(NationalCommunicationInstitutionalDeliveryProofResponse.class)));
        when(institutionalOperationsFacade.provasInstitucionais("uuid-2")).thenReturn(esperado);

        assertThat(service.provasInstitucionais("uuid-2")).isSameAs(esperado);
    }

    @Test
    void gatesInstitucionaisDelegaComOsMesmosArgumentos() {
        List<NationalCommunicationInstitutionalGateStateResponse> esperado = new ArrayList<>(List.of(mock(NationalCommunicationInstitutionalGateStateResponse.class)));
        when(institutionalOperationsFacade.gatesInstitucionais(20L, "uuid-3")).thenReturn(esperado);

        assertThat(service.gatesInstitucionais(20L, "uuid-3")).isSameAs(esperado);
    }

    @Test
    void listarEntregasInstitucionaisDelegaComOsMesmosArgumentos() {
        List<NationalCommunicationInstitutionalDeliveryQueueItemResponse> esperado = new ArrayList<>(List.of(mock(NationalCommunicationInstitutionalDeliveryQueueItemResponse.class)));
        when(institutionalOperationsFacade.listarEntregasInstitucionais(21L, "uuid-4")).thenReturn(esperado);

        assertThat(service.listarEntregasInstitucionais(21L, "uuid-4")).isSameAs(esperado);
    }

    @Test
    void listarDlqInstitucionalDelegaComOsMesmosArgumentos() {
        List<NationalCommunicationInstitutionalDeadLetterResponse> esperado = new ArrayList<>(List.of(mock(NationalCommunicationInstitutionalDeadLetterResponse.class)));
        when(institutionalOperationsFacade.listarDlqInstitucional(22L, "uuid-5")).thenReturn(esperado);

        assertThat(service.listarDlqInstitucional(22L, "uuid-5")).isSameAs(esperado);
    }

    @Test
    void reprocessarEntregaInstitucionalDelega() {
        var request = mock(NationalCommunicationInstitutionalReprocessDeliveryRequest.class);
        NationalCommunicationInstitutionalDeliveryQueueItemResponse esperado = mock(NationalCommunicationInstitutionalDeliveryQueueItemResponse.class);
        when(institutionalOperationsFacade.reprocessarEntregaInstitucional(request)).thenReturn(esperado);

        assertThat(service.reprocessarEntregaInstitucional(request)).isSameAs(esperado);
    }

    @Test
    void listarIntegracoesExternasDelegaComOsMesmosArgumentos() {
        List<NationalCommunicationInstitutionalExternalDispatchResponse> esperado = new ArrayList<>(List.of(mock(NationalCommunicationInstitutionalExternalDispatchResponse.class)));
        when(institutionalOperationsFacade.listarIntegracoesExternas(23L, "uuid-6")).thenReturn(esperado);

        assertThat(service.listarIntegracoesExternas(23L, "uuid-6")).isSameAs(esperado);
    }

    @Test
    void observabilidadeInstitucionalDelegaComOsMesmosArgumentos() {
        NationalCommunicationInstitutionalObservabilityDashboardResponse esperado = mock(NationalCommunicationInstitutionalObservabilityDashboardResponse.class);
        when(institutionalOperationsFacade.observabilidadeInstitucional(24L, "CE", DestinatarioInstitucionalKind.MINISTERIO_PUBLICO)).thenReturn(esperado);

        assertThat(service.observabilidadeInstitucional(24L, "CE", DestinatarioInstitucionalKind.MINISTERIO_PUBLICO)).isSameAs(esperado);
    }

    @Test
    void hardeningInstitucionalDevolveORelatorioDaFachada() {
        NationalCommunicationInstitutionalHardeningReportResponse relatorio = mock(NationalCommunicationInstitutionalHardeningReportResponse.class);
        when(institutionalOperationsFacade.hardeningInstitucional()).thenReturn(relatorio);

        assertThat(service.hardeningInstitucional()).isSameAs(relatorio);
    }
}
