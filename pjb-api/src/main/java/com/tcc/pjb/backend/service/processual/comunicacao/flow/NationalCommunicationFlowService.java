package com.tcc.pjb.backend.service.processual.comunicacao.flow;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.canonico.AtoCanonicoProcessualResolver;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.application.InstitutionalDeliveryQueueApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalDocumentSecurityGateApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.application.InstitutionalInboxApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.observability.application.InstitutionalCommunicationObservabilityApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.routing.MotorRoteamentoComunicacaoInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalFlowAnalyticsApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalWorkflowApplicationService;
import com.tcc.pjb.backend.core.comunicacao.judicial.CitacaoIntimacaoEngine;
import com.tcc.pjb.backend.core.comunicacao.processual.destinatario.application.DestinatarioProcessualResolverApplicationService;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.flow.NationalCommunicationAcknowledgeRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.flow.NationalCommunicationCanonicalActResolveRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.flow.NationalCommunicationCanonicalActResolveResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.flow.NationalCommunicationDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.flow.NationalCommunicationDispatchRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.flow.NationalCommunicationDispatchResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.flow.NationalCommunicationFallbackRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalAccessCheckRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalAccessCheckResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalMembershipResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalDelegateRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalDelegationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalSemanticTimelineEntryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalActionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalDeadLetterResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalDeliveryProofResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalDraftCreateRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalDraftResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalDraftReviewRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalDraftSubmitRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalExternalDispatchResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalFulfillRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalGateStateResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalReceiveRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalRedistributeRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalReprocessDeliveryRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalResolveRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalResolveResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalScienceRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalSubstitutionRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalAnalyticsDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalDeliveryQueueItemResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalInboxItemResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalObservabilityDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalTimelineEventResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalHardeningReportResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.routing.NationalCommunicationProcessualRecipientResolveRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.routing.NationalCommunicationProcessualRecipientResolveResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.routing.NationalCommunicationRoutingResolveRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.routing.NationalCommunicationRoutingResolveResponse;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.StatusComunicacaoInstitucional;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationsFacade;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NationalCommunicationFlowService {

    private final NationalCommunicationFlowFacade facade;

    public NationalCommunicationFlowService(CitacaoIntimacaoEngine citacaoIntimacaoEngine,
                                            ProcessoRepository processoRepository,
                                            WorkItemRepository workItemRepository,
                                            ProcessoLifecycleMachine processoLifecycleMachine,
                                            CurrentUserService currentUserService,
                                            PjbAuthorizationService authorizationService,
                                            AuditLedgerService auditLedgerService,
                                            CatalogoInstitucionalUnificadoService catalogoInstitucionalUnificadoService,
                                            AtoCanonicoProcessualResolver atoCanonicoProcessualResolver,
                                            MotorRoteamentoComunicacaoInstitucional motorRoteamentoComunicacaoInstitucional,
                                            InstitutionalInboxApplicationService institutionalInboxApplicationService,
                                            InstitutionalDeliveryQueueApplicationService institutionalDeliveryQueueApplicationService,
                                            InstitutionalWorkflowApplicationService institutionalWorkflowApplicationService,
                                            InstitutionalFlowAnalyticsApplicationService institutionalFlowAnalyticsApplicationService,
                                            DestinatarioProcessualResolverApplicationService destinatarioProcessualResolverApplicationService,
                                            InstitutionalDocumentSecurityGateApplicationService institutionalDocumentSecurityGateApplicationService,
                                            NationalCommunicationInstitutionalOperationsFacade institutionalOperationsFacade) {
        this.facade = new NationalCommunicationFlowFacade(
                citacaoIntimacaoEngine,
                processoRepository,
                workItemRepository,
                processoLifecycleMachine,
                currentUserService,
                authorizationService,
                auditLedgerService,
                catalogoInstitucionalUnificadoService,
                atoCanonicoProcessualResolver,
                motorRoteamentoComunicacaoInstitucional,
                institutionalInboxApplicationService,
                institutionalDeliveryQueueApplicationService,
                institutionalWorkflowApplicationService,
                institutionalFlowAnalyticsApplicationService,
                destinatarioProcessualResolverApplicationService,
                institutionalDocumentSecurityGateApplicationService,
                institutionalOperationsFacade
        );
    }
    public NationalCommunicationDispatchResponse expedir(NationalCommunicationDispatchRequest request) {
        return facade.expedir(request);
    }

    @Transactional(readOnly = true)
    public NationalCommunicationCanonicalActResolveResponse resolverAtoCanonico(NationalCommunicationCanonicalActResolveRequest request) {
        return facade.resolverAtoCanonico(request);
    }

    @Transactional(readOnly = true)
    public NationalCommunicationProcessualRecipientResolveResponse resolverDestinatarioProcessual(
            NationalCommunicationProcessualRecipientResolveRequest request
    ) {
        return facade.resolverDestinatarioProcessual(request);
    }

    @Transactional(readOnly = true)
    public NationalCommunicationInstitutionalResolveResponse resolverDestinoInstitucional(NationalCommunicationInstitutionalResolveRequest request) {
        return facade.resolverDestinoInstitucional(request);
    }

    @Transactional(readOnly = true)
    public NationalCommunicationRoutingResolveResponse resolverRoteamentoInstitucional(NationalCommunicationRoutingResolveRequest request) {
        return facade.resolverRoteamentoInstitucional(request);
    }
    public void acusarCiencia(NationalCommunicationAcknowledgeRequest request) {
        facade.acusarCiencia(request);
    }
    public NationalCommunicationInstitutionalDelegationResponse delegarInstitucional(NationalCommunicationInstitutionalDelegateRequest request) {
        return facade.delegarInstitucional(request);
    }
    public NationalCommunicationInstitutionalDelegationResponse substituirInstitucional(NationalCommunicationInstitutionalSubstitutionRequest request) {
        return facade.substituirInstitucional(request);
    }

    @Transactional(readOnly = true)
    public List<NationalCommunicationInstitutionalDelegationResponse> listarDelegacoesInstitucionais(String expedicaoUuid) {
        return facade.listarDelegacoesInstitucionais(expedicaoUuid);
    }
    public NationalCommunicationInstitutionalDraftResponse criarOuAtualizarMinutaInstitucional(NationalCommunicationInstitutionalDraftCreateRequest request) {
        return facade.criarOuAtualizarMinutaInstitucional(request);
    }
    public NationalCommunicationInstitutionalDraftResponse submeterMinutaInstitucional(NationalCommunicationInstitutionalDraftSubmitRequest request) {
        return facade.submeterMinutaInstitucional(request);
    }
    public NationalCommunicationInstitutionalDraftResponse aprovarMinutaInstitucional(NationalCommunicationInstitutionalDraftReviewRequest request) {
        return facade.aprovarMinutaInstitucional(request);
    }
    public NationalCommunicationInstitutionalDraftResponse rejeitarMinutaInstitucional(NationalCommunicationInstitutionalDraftReviewRequest request) {
        return facade.rejeitarMinutaInstitucional(request);
    }

    @Transactional(readOnly = true)
    public List<NationalCommunicationInstitutionalDraftResponse> listarMinutasInstitucionais(String expedicaoUuid) {
        return facade.listarMinutasInstitucionais(expedicaoUuid);
    }

    @Transactional(readOnly = true)
    public List<NationalCommunicationInstitutionalSemanticTimelineEntryResponse> timelineSemanticaInstitucional(String expedicaoUuid) {
        return facade.timelineSemanticaInstitucional(expedicaoUuid);
    }

    @Transactional(readOnly = true)
    public NationalCommunicationInstitutionalAnalyticsDashboardResponse analyticsInstitucional(Long processoId, String expedicaoUuid) {
        return facade.analyticsInstitucional(processoId, expedicaoUuid);
    }
    public NationalCommunicationDispatchResponse acionarFallback(NationalCommunicationFallbackRequest request) {
        return facade.acionarFallback(request);
    }

    @Transactional(readOnly = true)
    public java.util.List<NationalCommunicationInstitutionalMembershipResponse> minhasCaixasInstitucionais(DestinatarioInstitucionalKind destinatarioKind,
                                                                                                             String uf,
                                                                                                             String comarca) {
        return facade.minhasCaixasInstitucionais(destinatarioKind, uf, comarca);
    }

    @Transactional(readOnly = true)
    public NationalCommunicationInstitutionalAccessCheckResponse autorizarCaixaInstitucional(NationalCommunicationInstitutionalAccessCheckRequest request) {
        return facade.autorizarCaixaInstitucional(request);
    }

    @Transactional(readOnly = true)
    public java.util.List<NationalCommunicationInstitutionalInboxItemResponse> listarInboxInstitucional(StatusComunicacaoInstitucional status,
                                                                                                         Long processoId) {
        return facade.listarInboxInstitucional(status, processoId);
    }
    public NationalCommunicationInstitutionalActionResponse receberInboxInstitucional(NationalCommunicationInstitutionalReceiveRequest request) {
        return facade.receberInboxInstitucional(request);
    }
    public NationalCommunicationInstitutionalActionResponse redistribuirInboxInstitucional(NationalCommunicationInstitutionalRedistributeRequest request) {
        return facade.redistribuirInboxInstitucional(request);
    }
    public NationalCommunicationInstitutionalActionResponse certificarCienciaInstitucional(NationalCommunicationInstitutionalScienceRequest request) {
        return facade.certificarCienciaInstitucional(request);
    }
    public NationalCommunicationInstitutionalActionResponse cumprirInboxInstitucional(NationalCommunicationInstitutionalFulfillRequest request) {
        return facade.cumprirInboxInstitucional(request);
    }

    @Transactional(readOnly = true)
    public java.util.List<NationalCommunicationInstitutionalTimelineEventResponse> timelineInstitucional(String expedicaoUuid) {
        return facade.timelineInstitucional(expedicaoUuid);
    }

    @Transactional(readOnly = true)
    public java.util.List<NationalCommunicationInstitutionalDeliveryProofResponse> provasInstitucionais(String expedicaoUuid) {
        return facade.provasInstitucionais(expedicaoUuid);
    }

    @Transactional(readOnly = true)
    public java.util.List<NationalCommunicationInstitutionalGateStateResponse> gatesInstitucionais(Long processoId, String expedicaoUuid) {
        return facade.gatesInstitucionais(processoId, expedicaoUuid);
    }

    @Transactional(readOnly = true)
    public java.util.List<NationalCommunicationInstitutionalDeliveryQueueItemResponse> listarEntregasInstitucionais(Long processoId, String expedicaoUuid) {
        return facade.listarEntregasInstitucionais(processoId, expedicaoUuid);
    }

    @Transactional(readOnly = true)
    public java.util.List<NationalCommunicationInstitutionalDeadLetterResponse> listarDlqInstitucional(Long processoId, String expedicaoUuid) {
        return facade.listarDlqInstitucional(processoId, expedicaoUuid);
    }
    public NationalCommunicationInstitutionalDeliveryQueueItemResponse reprocessarEntregaInstitucional(
            NationalCommunicationInstitutionalReprocessDeliveryRequest request
    ) {
        return facade.reprocessarEntregaInstitucional(request);
    }

    @Transactional(readOnly = true)
    public java.util.List<NationalCommunicationInstitutionalExternalDispatchResponse> listarIntegracoesExternas(Long processoId, String expedicaoUuid) {
        return facade.listarIntegracoesExternas(processoId, expedicaoUuid);
    }

    @Transactional(readOnly = true)
    public NationalCommunicationInstitutionalObservabilityDashboardResponse observabilidadeInstitucional(
            Long processoId,
            String uf,
            DestinatarioInstitucionalKind destinatarioKind
    ) {
        return facade.observabilidadeInstitucional(processoId, uf, destinatarioKind);
    }

    @Transactional(readOnly = true)
    public NationalCommunicationInstitutionalHardeningReportResponse hardeningInstitucional() {
        return facade.hardeningInstitucional();
    }
    public NationalCommunicationDashboardResponse painel() {
        return facade.painel();
    }
}