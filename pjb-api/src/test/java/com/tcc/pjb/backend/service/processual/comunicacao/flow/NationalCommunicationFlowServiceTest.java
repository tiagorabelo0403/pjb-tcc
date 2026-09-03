package com.tcc.pjb.backend.service.processual.comunicacao.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.AutorizacaoCaixaInstitucionalService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.VinculoUsuarioCaixaInstitucionalResolver;
import com.tcc.pjb.backend.core.comunicacao.institucional.audit.application.InstitutionalCommunicationAuditApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.canonico.AtoCanonicoComunicacaoMapper;
import com.tcc.pjb.backend.core.comunicacao.institucional.canonico.AtoCanonicoProcessualResolver;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.application.InstitutionalDeliveryQueueApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalDocumentSecurityGateApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.application.InstitutionalCommunicationGateApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.hardening.application.InstitutionalCommunicationConcurrencyGuardService;
import com.tcc.pjb.backend.core.comunicacao.institucional.hardening.application.InstitutionalCommunicationHardeningApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.application.InstitutionalInboxApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.observability.application.InstitutionalCommunicationObservabilityApplicationService;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.access.InstitutionalRequestAccessContextFacadeService;
import com.tcc.pjb.backend.core.comunicacao.institucional.routing.MotorRoteamentoComunicacaoInstitucional;
import com.tcc.pjb.backend.core.comunicacao.judicial.CitacaoIntimacaoEngine;
import com.tcc.pjb.backend.core.comunicacao.judicial.ModalidadeExpedicaoJudicial;
import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;
import com.tcc.pjb.backend.core.comunicacao.processual.destinatario.application.DestinatarioProcessualResolverApplicationService;
import com.tcc.pjb.backend.core.comunicacao.processual.destinatario.domain.DestinatarioProcessual;
import com.tcc.pjb.backend.core.comunicacao.processual.destinatario.domain.ResolucaoDestinatarioProcessualResult;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.flow.NationalCommunicationDispatchRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioProcessualKind;
import com.tcc.pjb.backend.model.entity.enums.NationalCommunicationRecipientKind;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.TrilhoComunicacaoProcessual;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

class NationalCommunicationFlowServiceTest {

    @Test
    void shouldCreateFollowUpWorkItem() {
        CitacaoIntimacaoEngine engine = Mockito.mock(CitacaoIntimacaoEngine.class);
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        WorkItemRepository workItemRepository = Mockito.mock(WorkItemRepository.class);
        ProcessoLifecycleMachine lifecycleMachine = Mockito.mock(ProcessoLifecycleMachine.class);
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        AuditLedgerService auditLedgerService = Mockito.mock(AuditLedgerService.class);
        CatalogoInstitucionalUnificadoService catalogo = new CatalogoInstitucionalUnificadoService(new StaticListableBeanFactory().getBeanProvider(com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCatalogGovernanceOverlayService.class));
        VinculoUsuarioCaixaInstitucionalResolver vinculoResolver = Mockito.mock(VinculoUsuarioCaixaInstitucionalResolver.class);
        AutorizacaoCaixaInstitucionalService autorizacaoService = Mockito.mock(AutorizacaoCaixaInstitucionalService.class);
        AtoCanonicoProcessualResolver atoCanonicoResolver = new AtoCanonicoProcessualResolver(new AtoCanonicoComunicacaoMapper());
        Processo processo = new Processo();
        processo.setId(3L);
        processo.setNumeroProcesso("0003");
        Usuario usuario = new Usuario();
        usuario.setId(9L);
        usuario.setTipoUsuario(TipoUsuario.SERVIDOR_FORUM);
        when(processoRepository.findById(3L)).thenReturn(Optional.of(processo));
        when(currentUserService.getRequired()).thenReturn(usuario);
        when(engine.expedir(any())).thenReturn(new CitacaoIntimacaoEngine.ExpedicaoResponse(
                "uuid-x",
                3L,
                TipoComunicacaoJudicial.INTIMACAO_ADVOGADO,
                ModalidadeExpedicaoJudicial.DIGITAL_GOVBR_PUSH,
                com.tcc.pjb.backend.core.comunicacao.judicial.ExpedicaoJudicial.StatusExpedicao.EXPEDIDA,
                "***",
                "Nome",
                "GOVBR",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                List.of(),
                List.of(),
                "hash",
                "fund",
                false
        ));
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> {
            WorkItem item = invocation.getArgument(0);
            item.setId(77L);
            return item;
        });
        MotorRoteamentoComunicacaoInstitucional motorRoteamento = Mockito.mock(MotorRoteamentoComunicacaoInstitucional.class);
        InstitutionalInboxApplicationService inboxService = Mockito.mock(InstitutionalInboxApplicationService.class);
        InstitutionalCommunicationAuditApplicationService auditInstitutionalService = Mockito.mock(InstitutionalCommunicationAuditApplicationService.class);
        InstitutionalCommunicationGateApplicationService gateService = Mockito.mock(InstitutionalCommunicationGateApplicationService.class);
        InstitutionalDeliveryQueueApplicationService deliveryQueueService = Mockito.mock(InstitutionalDeliveryQueueApplicationService.class);
        InstitutionalCommunicationObservabilityApplicationService observabilityService = Mockito.mock(InstitutionalCommunicationObservabilityApplicationService.class);
        InstitutionalCommunicationConcurrencyGuardService concurrencyGuardService = Mockito.mock(InstitutionalCommunicationConcurrencyGuardService.class);
        InstitutionalCommunicationHardeningApplicationService hardeningApplicationService = Mockito.mock(InstitutionalCommunicationHardeningApplicationService.class);
        var workflowApplicationService = Mockito.mock(com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalWorkflowApplicationService.class);
        var flowAnalyticsApplicationService = Mockito.mock(com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalFlowAnalyticsApplicationService.class);
        DestinatarioProcessualResolverApplicationService destinatarioResolver = Mockito.mock(DestinatarioProcessualResolverApplicationService.class);
        InstitutionalDocumentSecurityGateApplicationService documentSecurityGateService = Mockito.mock(InstitutionalDocumentSecurityGateApplicationService.class);
        InstitutionalRequestAccessContextFacadeService requestAccessContextFacadeService = Mockito.mock(InstitutionalRequestAccessContextFacadeService.class);
        when(concurrencyGuardService.execute(any(), any(), any())).thenAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(2)).get());
        var institutionalOperationsFacade = new com.tcc.pjb.backend.service.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationsFacade(
                processoRepository,
                currentUserService,
                authorizationService,
                vinculoResolver,
                autorizacaoService,
                inboxService,
                auditInstitutionalService,
                gateService,
                deliveryQueueService,
                observabilityService,
                concurrencyGuardService,
                hardeningApplicationService,
                requestAccessContextFacadeService
        );
        when(destinatarioResolver.resolver(any())).thenReturn(new ResolucaoDestinatarioProcessualResult(
                new DestinatarioProcessual(
                        DestinatarioProcessualKind.ADVOGADO,
                        TrilhoComunicacaoProcessual.REPRESENTACAO_PROCESSUAL,
                        NationalCommunicationRecipientKind.ADVOGADO_OAB,
                        "11111111111",
                        "Adv",
                        "a@b.com",
                        null,
                        "12345",
                        null,
                        "CE",
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        false,
                        true,
                        true,
                        java.util.List.of(),
                        null
                ),
                TrilhoComunicacaoProcessual.REPRESENTACAO_PROCESSUAL,
                true,
                false,
                true,
                true,
                java.util.List.of(),
                null
        ));
        NationalCommunicationFlowService service = new NationalCommunicationFlowService(
                engine,
                processoRepository,
                workItemRepository,
                lifecycleMachine,
                currentUserService,
                authorizationService,
                auditLedgerService,
                catalogo,
                atoCanonicoResolver,
                motorRoteamento,
                inboxService,
                deliveryQueueService,
                workflowApplicationService,
                flowAnalyticsApplicationService,
                destinatarioResolver,
                documentSecurityGateService,
                institutionalOperationsFacade
        );
        var response = service.expedir(new NationalCommunicationDispatchRequest(
                3L,
                TipoComunicacaoJudicial.INTIMACAO_ADVOGADO,
                NationalCommunicationRecipientKind.ADVOGADO_OAB,
                "11111111111",
                "Adv",
                "a@b.com",
                null,
                null,
                "12345",
                "CE",
                null,
                null,
                true,
                false,
                false,
                false,
                false,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                true,
                false,
                "conteudo",
                "fund"
        ));
        assertEquals(77L, response.workItemId());
    }
}
