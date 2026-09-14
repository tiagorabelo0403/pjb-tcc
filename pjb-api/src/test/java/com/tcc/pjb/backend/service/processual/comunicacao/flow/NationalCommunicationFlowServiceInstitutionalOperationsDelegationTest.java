package com.tcc.pjb.backend.service.processual.comunicacao.flow;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalFulfillRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalReceiveRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalRedistributeRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalReprocessDeliveryRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalScienceRequest;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.StatusComunicacaoInstitucional;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationsFacade;
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
        service.minhasCaixasInstitucionais(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO, "CE", "Fortaleza");
        verify(institutionalOperationsFacade).minhasCaixasInstitucionais(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO, "CE", "Fortaleza");
    }

    @Test
    void autorizarCaixaInstitucionalDelega() {
        var request = mock(NationalCommunicationInstitutionalAccessCheckRequest.class);
        service.autorizarCaixaInstitucional(request);
        verify(institutionalOperationsFacade).autorizarCaixaInstitucional(request);
    }

    @Test
    void listarInboxInstitucionalDelegaComOsMesmosArgumentos() {
        service.listarInboxInstitucional(StatusComunicacaoInstitucional.RECEBIDA, 10L);
        verify(institutionalOperationsFacade).listarInboxInstitucional(StatusComunicacaoInstitucional.RECEBIDA, 10L);
    }

    @Test
    void receberInboxInstitucionalDelega() {
        var request = mock(NationalCommunicationInstitutionalReceiveRequest.class);
        service.receberInboxInstitucional(request);
        verify(institutionalOperationsFacade).receberInboxInstitucional(request);
    }

    @Test
    void redistribuirInboxInstitucionalDelega() {
        var request = mock(NationalCommunicationInstitutionalRedistributeRequest.class);
        service.redistribuirInboxInstitucional(request);
        verify(institutionalOperationsFacade).redistribuirInboxInstitucional(request);
    }

    @Test
    void certificarCienciaInstitucionalDelega() {
        var request = mock(NationalCommunicationInstitutionalScienceRequest.class);
        service.certificarCienciaInstitucional(request);
        verify(institutionalOperationsFacade).certificarCienciaInstitucional(request);
    }

    @Test
    void cumprirInboxInstitucionalDelega() {
        var request = mock(NationalCommunicationInstitutionalFulfillRequest.class);
        service.cumprirInboxInstitucional(request);
        verify(institutionalOperationsFacade).cumprirInboxInstitucional(request);
    }

    @Test
    void timelineInstitucionalDelegaComOMesmoExpedicaoUuid() {
        service.timelineInstitucional("uuid-1");
        verify(institutionalOperationsFacade).timelineInstitucional("uuid-1");
    }

    @Test
    void provasInstitucionaisDelegaComOMesmoExpedicaoUuid() {
        service.provasInstitucionais("uuid-2");
        verify(institutionalOperationsFacade).provasInstitucionais("uuid-2");
    }

    @Test
    void gatesInstitucionaisDelegaComOsMesmosArgumentos() {
        service.gatesInstitucionais(20L, "uuid-3");
        verify(institutionalOperationsFacade).gatesInstitucionais(20L, "uuid-3");
    }

    @Test
    void listarEntregasInstitucionaisDelegaComOsMesmosArgumentos() {
        service.listarEntregasInstitucionais(21L, "uuid-4");
        verify(institutionalOperationsFacade).listarEntregasInstitucionais(21L, "uuid-4");
    }

    @Test
    void listarDlqInstitucionalDelegaComOsMesmosArgumentos() {
        service.listarDlqInstitucional(22L, "uuid-5");
        verify(institutionalOperationsFacade).listarDlqInstitucional(22L, "uuid-5");
    }

    @Test
    void reprocessarEntregaInstitucionalDelega() {
        var request = mock(NationalCommunicationInstitutionalReprocessDeliveryRequest.class);
        service.reprocessarEntregaInstitucional(request);
        verify(institutionalOperationsFacade).reprocessarEntregaInstitucional(request);
    }

    @Test
    void listarIntegracoesExternasDelegaComOsMesmosArgumentos() {
        service.listarIntegracoesExternas(23L, "uuid-6");
        verify(institutionalOperationsFacade).listarIntegracoesExternas(23L, "uuid-6");
    }

    @Test
    void observabilidadeInstitucionalDelegaComOsMesmosArgumentos() {
        service.observabilidadeInstitucional(24L, "CE", DestinatarioInstitucionalKind.MINISTERIO_PUBLICO);
        verify(institutionalOperationsFacade).observabilidadeInstitucional(24L, "CE", DestinatarioInstitucionalKind.MINISTERIO_PUBLICO);
    }

    @Test
    void hardeningInstitucionalDelega() {
        service.hardeningInstitucional();
        verify(institutionalOperationsFacade).hardeningInstitucional();
    }
}
