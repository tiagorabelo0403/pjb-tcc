package com.tcc.pjb.backend.service.processual.comunicacao.flow;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.VinculoUsuarioCaixaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.audit.domain.InstitutionalDeliveryProof;
import com.tcc.pjb.backend.core.comunicacao.institucional.audit.domain.InstitutionalTimelineEvent;
import com.tcc.pjb.backend.core.comunicacao.institucional.canonico.AtoCanonicoProcessualResolver;
import com.tcc.pjb.backend.core.comunicacao.institucional.canonico.ResolucaoAtoCanonicoRequest;
import com.tcc.pjb.backend.core.comunicacao.institucional.canonico.ResolucaoAtoCanonicoResult;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.application.InstitutionalDeliveryQueueApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeadLetterEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryJob;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.domain.InstitutionalGateState;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalDocumentSecurityGateApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.hardening.domain.InstitutionalCommunicationHardeningReport;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.application.InstitutionalInboxActionResult;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.application.InstitutionalInboxApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain.InstitutionalExternalDispatch;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.ResolucaoDestinoInstitucionalRequest;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.ResolucaoDestinoInstitucionalResult;
import com.tcc.pjb.backend.core.comunicacao.institucional.observability.domain.InstitutionalObservabilityDashboard;
import com.tcc.pjb.backend.core.comunicacao.institucional.routing.MotorRoteamentoComunicacaoInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.routing.ResolucaoRoteamentoInstitucionalRequest;
import com.tcc.pjb.backend.core.comunicacao.institucional.routing.ResolucaoRoteamentoInstitucionalResult;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalFlowAnalyticsApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalWorkflowApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalDelegationAssignment;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalDraftManifestation;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalFlowAnalyticsBucket;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalFlowAnalyticsDashboard;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalSemanticTimelineEntry;
import com.tcc.pjb.backend.core.comunicacao.judicial.CitacaoIntimacaoEngine;
import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;
import com.tcc.pjb.backend.core.comunicacao.processual.destinatario.application.DestinatarioProcessualResolverApplicationService;
import com.tcc.pjb.backend.core.comunicacao.processual.destinatario.domain.DestinatarioProcessual;
import com.tcc.pjb.backend.core.comunicacao.processual.destinatario.domain.ResolucaoDestinatarioProcessualRequest;
import com.tcc.pjb.backend.core.comunicacao.processual.destinatario.domain.ResolucaoDestinatarioProcessualResult;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
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
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalAnalyticsBucketResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalAnalyticsDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalDeliveryQueueItemResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalInboxItemResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalObservabilityBucketResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalObservabilityDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalTimelineEventResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalHardeningFindingResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalHardeningReportResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.routing.NationalCommunicationProcessualRecipientResolveRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.routing.NationalCommunicationProcessualRecipientResolveResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.routing.NationalCommunicationRoutingResolveRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.routing.NationalCommunicationRoutingResolveResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalSensitiveAct;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationsFacade;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

final class NationalCommunicationFlowFacade {

    private final CitacaoIntimacaoEngine citacaoIntimacaoEngine;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final ProcessoLifecycleMachine processoLifecycleMachine;
    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final AuditLedgerService auditLedgerService;
    private final CatalogoInstitucionalUnificadoService catalogoInstitucionalUnificadoService;
    private final AtoCanonicoProcessualResolver atoCanonicoProcessualResolver;
    private final MotorRoteamentoComunicacaoInstitucional motorRoteamentoComunicacaoInstitucional;
    private final InstitutionalInboxApplicationService institutionalInboxApplicationService;
    private final InstitutionalDeliveryQueueApplicationService institutionalDeliveryQueueApplicationService;
    private final InstitutionalWorkflowApplicationService institutionalWorkflowApplicationService;
    private final InstitutionalFlowAnalyticsApplicationService institutionalFlowAnalyticsApplicationService;
    private final DestinatarioProcessualResolverApplicationService destinatarioProcessualResolverApplicationService;
    private final InstitutionalDocumentSecurityGateApplicationService institutionalDocumentSecurityGateApplicationService;
    private final NationalCommunicationInstitutionalOperationsFacade institutionalOperationsFacade;

    NationalCommunicationFlowFacade(CitacaoIntimacaoEngine citacaoIntimacaoEngine,
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
        this.citacaoIntimacaoEngine = Objects.requireNonNull(citacaoIntimacaoEngine);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.processoLifecycleMachine = Objects.requireNonNull(processoLifecycleMachine);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.catalogoInstitucionalUnificadoService = Objects.requireNonNull(catalogoInstitucionalUnificadoService);
        this.atoCanonicoProcessualResolver = Objects.requireNonNull(atoCanonicoProcessualResolver);
        this.motorRoteamentoComunicacaoInstitucional = Objects.requireNonNull(motorRoteamentoComunicacaoInstitucional);
        this.institutionalInboxApplicationService = Objects.requireNonNull(institutionalInboxApplicationService);
        this.institutionalDeliveryQueueApplicationService = Objects.requireNonNull(institutionalDeliveryQueueApplicationService);
        this.institutionalWorkflowApplicationService = Objects.requireNonNull(institutionalWorkflowApplicationService);
        this.institutionalFlowAnalyticsApplicationService = Objects.requireNonNull(institutionalFlowAnalyticsApplicationService);
        this.destinatarioProcessualResolverApplicationService = Objects.requireNonNull(destinatarioProcessualResolverApplicationService);
        this.institutionalDocumentSecurityGateApplicationService = Objects.requireNonNull(institutionalDocumentSecurityGateApplicationService);
        this.institutionalOperationsFacade = Objects.requireNonNull(institutionalOperationsFacade);
    }

    @Transactional
    public NationalCommunicationDispatchResponse expedir(NationalCommunicationDispatchRequest request) {
        Objects.requireNonNull(request);
        Processo processo = processoRepository.findById(request.processoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoId()));
        authorizationService.requireReadProcesso(processo);
        Usuario usuario = currentUserService.getRequired();
        authorizationService.requireRoleAny(
                usuario,
                "ROLE_ADMIN",
                "ROLE_ADMINISTRADOR",
                "ROLE_SERVIDOR",
                "ROLE_SERVIDOR_FORUM",
                "ROLE_JUIZ",
                "ROLE_MAGISTRADO",
                "ROLE_DESEMBARGADOR",
                "ROLE_MINISTRO",
                "ROLE_DEFENSOR_PUBLICO",
                "ROLE_PROCURADOR"
        );
        var institutionalDocumentGate = request.unidadeInstitucionalCodigo() == null || request.unidadeInstitucionalCodigo().isBlank()
                ? null
                : institutionalDocumentSecurityGateApplicationService.enforce(
                        request.unidadeInstitucionalCodigo(),
                        null,
                        InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO,
                        "EXPEDIR_COMUNICACAO_INSTITUCIONAL",
                        false);
        ResolucaoAtoCanonicoResult atoCanonicoPreview = resolverAtoCanonicoInterno(processo, null);
        ResolucaoDestinatarioProcessualResult destinatarioPreview = resolverDestinatarioProcessualInterno(request, processo, atoCanonicoPreview);
        ResolucaoDestinoInstitucionalResult institucionalPreview = preResolverInstitucional(request, processo, atoCanonicoPreview, destinatarioPreview).orElse(null);
        ResolucaoRoteamentoInstitucionalResult roteamentoPreview = preResolverRoteamentoInstitucional(request, processo, atoCanonicoPreview, destinatarioPreview).orElse(null);
        TipoComunicacaoJudicial tipoComunicacaoEfetiva = roteamentoPreview == null ? request.tipoComunicacao() : roteamentoPreview.tipoComunicacaoEfetiva();
        boolean forcarDigital = Boolean.TRUE.equals(request.forcarDigital()) || (roteamentoPreview != null && roteamentoPreview.planoEntrega().forcarDigital());
        boolean forcarOficial = Boolean.TRUE.equals(request.forcarOficial()) || (roteamentoPreview != null && roteamentoPreview.planoEntrega().forcarOficial());
        var response = citacaoIntimacaoEngine.expedir(new CitacaoIntimacaoEngine.ExpedicaoRequest(
                request.processoId(),
                tipoComunicacaoEfetiva,
                toDestinatario(destinatarioPreview, request),
                request.conteudoDoAto(),
                mergeRecipientFundamento(mergeFundamentoAdicional(request.fundamentoAdicional(), institucionalPreview, atoCanonicoPreview, roteamentoPreview), destinatarioPreview),
                forcarDigital,
                forcarOficial,
                usuario.isMagistrado() ? usuario.getId() : null,
                usuario.isServidorJudiciario() ? usuario.getId() : null
        ));
        processoLifecycleMachine.apply(processo, ProcessoLifecycleAction.EXPEDIR_INTIMACAO);
        processoRepository.save(processo);
        WorkItem workItemSeguimento = criarWorkItemSeguimento(processo, usuario, tipoComunicacaoEfetiva, response);
        Long workItemId = workItemSeguimento.getId();
        auditLedgerService.appendSafely("COMMUNICATION_FLOW_DISPATCH", "PROCESSO", String.valueOf(processo.getId()), response.hashIntegridade());
        auditLedgerService.appendSafely("COMMUNICATION_FLOW_RECIPIENT_ROUTE", "PROCESSO", String.valueOf(processo.getId()), destinatarioPreview.hashResolucao());
        if (institucionalPreview != null) {
            auditLedgerService.appendSafely("COMMUNICATION_FLOW_INSTITUTIONAL_ROUTE", "PROCESSO", String.valueOf(processo.getId()), institucionalPreview.alvo().hashResolucao());
        }
        if (roteamentoPreview != null) {
            auditLedgerService.appendSafely("COMMUNICATION_FLOW_DELIVERY_ROUTE", "PROCESSO", String.valueOf(processo.getId()), roteamentoPreview.hashResolucao());
        }
        if (atoCanonicoPreview != null) {
            auditLedgerService.appendSafely("COMMUNICATION_FLOW_CANONICAL_ACT", "PROCESSO", String.valueOf(processo.getId()), atoCanonicoPreview.hashResolucao());
        }
        if (institutionalDocumentGate != null) {
            auditLedgerService.appendSafely("COMMUNICATION_FLOW_DOCUMENT_GATE", "PROCESSO", String.valueOf(processo.getId()), institutionalDocumentGate.operationCode() + ":" + institutionalDocumentGate.nominationId() + ":" + institutionalDocumentGate.allowed());
        }
        if (roteamentoPreview != null) {
            institutionalInboxApplicationService.disponibilizar(processo, response, roteamentoPreview);
            institutionalDeliveryQueueApplicationService.enfileirar(processo, response, roteamentoPreview);
        }
        return new NationalCommunicationDispatchResponse(
                response.expedicaoUuid(),
                response.processoId(),
                response.tipoComunicacao().name(),
                response.modalidadeEscolhida().name(),
                response.status().name(),
                response.destinatarioDocumento(),
                response.destinatarioNome(),
                response.canalDigitalUtilizado(),
                response.expedidaEm(),
                response.presuncaoEntregaEm(),
                response.alertas(),
                response.cascataModalidades(),
                response.hashIntegridade(),
                response.fundamentacaoLegal(),
                response.antiEvasaoAtivado(),
                workItemId
        );
    }

    @Transactional(readOnly = true)
    public NationalCommunicationCanonicalActResolveResponse resolverAtoCanonico(NationalCommunicationCanonicalActResolveRequest request) {
        Objects.requireNonNull(request);
        Processo processo = request.processoId() == null ? null : processoRepository.findById(request.processoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoId()));
        if (processo != null) {
            authorizationService.requireReadProcesso(processo);
        }
        ResolucaoAtoCanonicoResult result = resolverAtoCanonicoInterno(processo, request);
        return toCanonicalResponse(result);
    }

    @Transactional(readOnly = true)
    public NationalCommunicationProcessualRecipientResolveResponse resolverDestinatarioProcessual(NationalCommunicationProcessualRecipientResolveRequest request) {
        Objects.requireNonNull(request);
        Processo processo = request.processoId() == null ? null : processoRepository.findById(request.processoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoId()));
        if (processo != null) {
            authorizationService.requireReadProcesso(processo);
        }
        ResolucaoDestinatarioProcessualResult result = destinatarioProcessualResolverApplicationService.resolver(new ResolucaoDestinatarioProcessualRequest(
                processo != null ? processo.getId() : request.processoId(),
                processo != null ? processo.getNumeroProcesso() : null,
                request.tipoComunicacao(),
                request.destinatarioTipo(),
                request.destinatarioProcessualTipo(),
                request.destinatarioInstitucionalKind(),
                request.papelProcessualInstitucional(),
                request.unidadeInstitucionalCodigo(),
                request.documento(),
                request.nome(),
                request.email(),
                request.telefone(),
                request.oabNumero(),
                request.govbrAccountId(),
                firstNonBlank(request.uf(), inferUf(processo, null)),
                firstNonBlank(request.comarca(), inferComarca(processo)),
                firstNonBlank(request.foro(), inferForo(processo)),
                request.possuiContaGovBr(),
                request.possuiAdvogado(),
                request.fazendaPublica(),
                request.intimacaoPessoalInstitucional(),
                request.tipoComunicacao() == null ? null : request.tipoComunicacao().isCitacao(),
                request.tipoComunicacao() == null ? null : request.tipoComunicacao().isIntimacao()
        ));
        return toRecipientResponse(result);
    }

    @Transactional(readOnly = true)
    public NationalCommunicationInstitutionalResolveResponse resolverDestinoInstitucional(NationalCommunicationInstitutionalResolveRequest request) {
        Objects.requireNonNull(request);
        Processo processo = request.processoId() == null ? null : processoRepository.findById(request.processoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoId()));
        if (processo != null) {
            authorizationService.requireReadProcesso(processo);
        }
        ResolucaoDestinoInstitucionalResult result = catalogoInstitucionalUnificadoService.resolver(buildInstitutionalRequest(request, processo));
        return toInstitutionalResponse(result);
    }

    @Transactional(readOnly = true)
    public NationalCommunicationRoutingResolveResponse resolverRoteamentoInstitucional(NationalCommunicationRoutingResolveRequest request) {
        Objects.requireNonNull(request);
        Processo processo = request.processoId() == null ? null : processoRepository.findById(request.processoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoId()));
        if (processo != null) {
            authorizationService.requireReadProcesso(processo);
        }
        ResolucaoRoteamentoInstitucionalResult result = motorRoteamentoComunicacaoInstitucional.resolver(buildRoutingRequest(request, processo));
        return toRoutingResponse(result);
    }

    @Transactional
    public void acusarCiencia(NationalCommunicationAcknowledgeRequest request) {
        citacaoIntimacaoEngine.processarAcuseRecebimento(new CitacaoIntimacaoEngine.AcuseRecebimentoRequest(
                request.expedicaoUuid(),
                request.tokenAcuse(),
                request.ipOrigem(),
                request.deviceFingerprint(),
                request.govbrSessionToken()
        ));
        auditLedgerService.appendSafely("COMMUNICATION_FLOW_ACKNOWLEDGED", "EXPEDICAO_JUDICIAL", request.expedicaoUuid(), request.tokenAcuse());
    }

    @Transactional
    public NationalCommunicationInstitutionalDelegationResponse delegarInstitucional(NationalCommunicationInstitutionalDelegateRequest request) {
        Objects.requireNonNull(request);
        InstitutionalDelegationAssignment result = institutionalWorkflowApplicationService.delegar(request.expedicaoUuid(), request.delegadoUsuarioId(), request.capacidades(), request.horasVigencia(), request.motivo());
        return toDelegationResponse(result);
    }

    @Transactional
    public NationalCommunicationInstitutionalDelegationResponse substituirInstitucional(NationalCommunicationInstitutionalSubstitutionRequest request) {
        Objects.requireNonNull(request);
        InstitutionalDelegationAssignment result = institutionalWorkflowApplicationService.substituir(request.expedicaoUuid(), request.substitutoUsuarioId(), request.horasVigencia(), request.motivo());
        return toDelegationResponse(result);
    }

    @Transactional(readOnly = true)
    public List<NationalCommunicationInstitutionalDelegationResponse> listarDelegacoesInstitucionais(String expedicaoUuid) {
        return institutionalWorkflowApplicationService.listarDelegacoes(expedicaoUuid).stream().map(this::toDelegationResponse).toList();
    }

    @Transactional
    public NationalCommunicationInstitutionalDraftResponse criarOuAtualizarMinutaInstitucional(NationalCommunicationInstitutionalDraftCreateRequest request) {
        Objects.requireNonNull(request);
        return toDraftResponse(institutionalWorkflowApplicationService.criarOuAtualizarMinuta(request.expedicaoUuid(), request.titulo(), request.conteudo(), request.observacoes()));
    }

    @Transactional
    public NationalCommunicationInstitutionalDraftResponse submeterMinutaInstitucional(NationalCommunicationInstitutionalDraftSubmitRequest request) {
        Objects.requireNonNull(request);
        return toDraftResponse(institutionalWorkflowApplicationService.submeterMinuta(request.expedicaoUuid(), request.draftId(), request.aprovadorUsuarioId(), request.observacoes()));
    }

    @Transactional
    public NationalCommunicationInstitutionalDraftResponse aprovarMinutaInstitucional(NationalCommunicationInstitutionalDraftReviewRequest request) {
        Objects.requireNonNull(request);
        return toDraftResponse(institutionalWorkflowApplicationService.aprovarMinuta(request.expedicaoUuid(), request.draftId(), request.observacoes(), Boolean.TRUE.equals(request.autoCumprir())));
    }

    @Transactional
    public NationalCommunicationInstitutionalDraftResponse rejeitarMinutaInstitucional(NationalCommunicationInstitutionalDraftReviewRequest request) {
        Objects.requireNonNull(request);
        return toDraftResponse(institutionalWorkflowApplicationService.rejeitarMinuta(request.expedicaoUuid(), request.draftId(), request.observacoes()));
    }

    @Transactional(readOnly = true)
    public List<NationalCommunicationInstitutionalDraftResponse> listarMinutasInstitucionais(String expedicaoUuid) {
        return institutionalWorkflowApplicationService.listarMinutas(expedicaoUuid).stream().map(this::toDraftResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<NationalCommunicationInstitutionalSemanticTimelineEntryResponse> timelineSemanticaInstitucional(String expedicaoUuid) {
        return institutionalFlowAnalyticsApplicationService.timelineSemantica(expedicaoUuid).stream().map(this::toSemanticTimelineResponse).toList();
    }

    @Transactional(readOnly = true)
    public NationalCommunicationInstitutionalAnalyticsDashboardResponse analyticsInstitucional(Long processoId, String expedicaoUuid) {
        return toAnalyticsResponse(institutionalFlowAnalyticsApplicationService.dashboard(processoId, expedicaoUuid));
    }

    @Transactional
    public NationalCommunicationDispatchResponse acionarFallback(NationalCommunicationFallbackRequest request) {
        var response = citacaoIntimacaoEngine.registrarFrustracaoEAcionarFallback(request.expedicaoUuid(), request.motivoFrustracao());
        auditLedgerService.appendSafely("COMMUNICATION_FLOW_FALLBACK", "EXPEDICAO_JUDICIAL", request.expedicaoUuid(), request.motivoFrustracao());
        return new NationalCommunicationDispatchResponse(
                response.expedicaoUuid(),
                response.processoId(),
                response.tipoComunicacao().name(),
                response.modalidadeEscolhida().name(),
                response.status().name(),
                response.destinatarioDocumento(),
                response.destinatarioNome(),
                response.canalDigitalUtilizado(),
                response.expedidaEm(),
                response.presuncaoEntregaEm(),
                response.alertas(),
                response.cascataModalidades(),
                response.hashIntegridade(),
                response.fundamentacaoLegal(),
                response.antiEvasaoAtivado(),
                null
        );
    }

    @Transactional(readOnly = true)
    public java.util.List<NationalCommunicationInstitutionalMembershipResponse> minhasCaixasInstitucionais(DestinatarioInstitucionalKind destinatarioKind,
                                                                                                             String uf,
                                                                                                             String comarca) {
        return institutionalOperationsFacade.minhasCaixasInstitucionais(destinatarioKind, uf, comarca);
    }

    @Transactional(readOnly = true)
    public NationalCommunicationInstitutionalAccessCheckResponse autorizarCaixaInstitucional(NationalCommunicationInstitutionalAccessCheckRequest request) {
        return institutionalOperationsFacade.autorizarCaixaInstitucional(request);
    }

    @Transactional(readOnly = true)
    public java.util.List<NationalCommunicationInstitutionalInboxItemResponse> listarInboxInstitucional(StatusComunicacaoInstitucional status,
                                                                                                         Long processoId) {
        return institutionalOperationsFacade.listarInboxInstitucional(status, processoId);
    }

    @Transactional
    public NationalCommunicationInstitutionalActionResponse receberInboxInstitucional(NationalCommunicationInstitutionalReceiveRequest request) {
        return institutionalOperationsFacade.receberInboxInstitucional(request);
    }

    @Transactional
    public NationalCommunicationInstitutionalActionResponse redistribuirInboxInstitucional(NationalCommunicationInstitutionalRedistributeRequest request) {
        return institutionalOperationsFacade.redistribuirInboxInstitucional(request);
    }

    @Transactional
    public NationalCommunicationInstitutionalActionResponse certificarCienciaInstitucional(NationalCommunicationInstitutionalScienceRequest request) {
        return institutionalOperationsFacade.certificarCienciaInstitucional(request);
    }

    @Transactional
    public NationalCommunicationInstitutionalActionResponse cumprirInboxInstitucional(NationalCommunicationInstitutionalFulfillRequest request) {
        return institutionalOperationsFacade.cumprirInboxInstitucional(request);
    }

    @Transactional(readOnly = true)
    public java.util.List<NationalCommunicationInstitutionalTimelineEventResponse> timelineInstitucional(String expedicaoUuid) {
        return institutionalOperationsFacade.timelineInstitucional(expedicaoUuid);
    }

    @Transactional(readOnly = true)
    public java.util.List<NationalCommunicationInstitutionalDeliveryProofResponse> provasInstitucionais(String expedicaoUuid) {
        return institutionalOperationsFacade.provasInstitucionais(expedicaoUuid);
    }

    @Transactional(readOnly = true)
    public java.util.List<NationalCommunicationInstitutionalGateStateResponse> gatesInstitucionais(Long processoId, String expedicaoUuid) {
        return institutionalOperationsFacade.gatesInstitucionais(processoId, expedicaoUuid);
    }

    @Transactional(readOnly = true)
    public java.util.List<NationalCommunicationInstitutionalDeliveryQueueItemResponse> listarEntregasInstitucionais(Long processoId, String expedicaoUuid) {
        return institutionalOperationsFacade.listarEntregasInstitucionais(processoId, expedicaoUuid);
    }

    @Transactional(readOnly = true)
    public java.util.List<NationalCommunicationInstitutionalDeadLetterResponse> listarDlqInstitucional(Long processoId, String expedicaoUuid) {
        return institutionalOperationsFacade.listarDlqInstitucional(processoId, expedicaoUuid);
    }

    @Transactional
    public NationalCommunicationInstitutionalDeliveryQueueItemResponse reprocessarEntregaInstitucional(NationalCommunicationInstitutionalReprocessDeliveryRequest request) {
        return institutionalOperationsFacade.reprocessarEntregaInstitucional(request);
    }

    @Transactional(readOnly = true)
    public java.util.List<NationalCommunicationInstitutionalExternalDispatchResponse> listarIntegracoesExternas(Long processoId, String expedicaoUuid) {
        return institutionalOperationsFacade.listarIntegracoesExternas(processoId, expedicaoUuid);
    }

    @Transactional(readOnly = true)
    public NationalCommunicationInstitutionalObservabilityDashboardResponse observabilidadeInstitucional(Long processoId, String uf, DestinatarioInstitucionalKind destinatarioKind) {
        return institutionalOperationsFacade.observabilidadeInstitucional(processoId, uf, destinatarioKind);
    }

    @Transactional(readOnly = true)
    public NationalCommunicationInstitutionalHardeningReportResponse hardeningInstitucional() {
        return institutionalOperationsFacade.hardeningInstitucional();
    }

    public NationalCommunicationDashboardResponse painel() {
        var painel = citacaoIntimacaoEngine.gerarPainel();
        return new NationalCommunicationDashboardResponse(
                painel.totalExpedidas(),
                painel.totalEntregues(),
                painel.totalPresumidas(),
                painel.totalFrustradas(),
                painel.totalPendentesOficial(),
                painel.totalEvasoes(),
                painel.totalEscalonadas(),
                painel.geradoEm(),
                painel.hashIntegridade()
        );
    }

    private WorkItem criarWorkItemSeguimento(Processo processo,
                                             Usuario usuario,
                                             TipoComunicacaoJudicial tipoComunicacao,
                                             CitacaoIntimacaoEngine.ExpedicaoResponse response) {
        WorkItem workItem = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(UUID.nameUUIDFromBytes((response.expedicaoUuid() + "|FOLLOWUP").getBytes(StandardCharsets.UTF_8)).toString())
                .type(tipoComunicacao.isCitacao() ? WorkItemType.CITACAO : WorkItemType.INTIMACAO)
                .titulo(tipoComunicacao.getDescricao() + " — " + processo.getNumeroProcesso())
                .descricao(response.fundamentacaoLegal())
                .queueCode(response.modalidadeEscolhida().isExigeOficial() ? "OFICIAL_JUSTICA_CUMPRIMENTO" : "SECRETARIA_COMUNICACOES")
                .inboxKey(response.modalidadeEscolhida().isExigeOficial() ? "OFICIAL_JUSTICA" : "SECRETARIA_INTIMACOES")
                .assignedRole(response.modalidadeEscolhida().isExigeOficial() ? TipoUsuario.OFICIAL_JUSTICA : TipoUsuario.SERVIDOR_FORUM)
                .status(WorkItemStatus.PENDENTE)
                .prioridade(tipoComunicacao.isCitacao() ? 1 : 2)
                .blocking(tipoComunicacao.isCitacao())
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal(response.fundamentacaoLegal())
                .dueAt(response.presuncaoEntregaEm() == null ? Instant.now().plus(72, ChronoUnit.HOURS) : response.presuncaoEntregaEm())
                .build();
        return workItemRepository.save(workItem);
    }

    private CitacaoIntimacaoEngine.PerfilDestinatario toDestinatario(ResolucaoDestinatarioProcessualResult resolved,
                                                                     NationalCommunicationDispatchRequest request) {
        DestinatarioProcessual destinatario = resolved.destinatario();
        String documento = destinatario.documentoPrincipal();
        if (destinatario.destinatarioInstitucionalKind() != null) {
            return switch (destinatario.destinatarioInstitucionalKind()) {
                case MINISTERIO_PUBLICO -> new CitacaoIntimacaoEngine.PerfilDestinatario.MinisterioPublico(
                        documento,
                        firstNonBlank(request.oabNumero(), destinatario.unidadeInstitucionalCodigo()),
                        firstNonBlank(destinatario.email(), request.email())
                );
                case DEFENSORIA_PUBLICA -> new CitacaoIntimacaoEngine.PerfilDestinatario.DefensorPublico(
                        documento,
                        firstNonBlank(request.oabNumero(), destinatario.unidadeInstitucionalCodigo()),
                        firstNonBlank(destinatario.email(), request.email())
                );
                case ADVOCACIA_PUBLICA,
                        FAZENDA_PUBLICA -> new CitacaoIntimacaoEngine.PerfilDestinatario.FazendaPublica(
                        documento,
                        firstNonBlank(request.razaoSocial(), destinatario.nomeExibicao()),
                        firstNonBlank(destinatario.email(), request.email()),
                        firstNonBlank(request.ente(), destinatario.destinatarioInstitucionalKind().name())
                );
                case JUIZO_DEPRECADO,
                        ORGAO_JUDICIAL_EXTERNO -> new CitacaoIntimacaoEngine.PerfilDestinatario.JuizoDeprecado(
                        firstNonBlank(destinatario.uf(), request.uf()),
                        firstNonBlank(destinatario.comarca(), request.comarca()),
                        firstNonBlank(destinatario.uf(), request.uf()),
                        firstNonBlank(destinatario.email(), request.email())
                );
                default -> new CitacaoIntimacaoEngine.PerfilDestinatario.PessoaJuridica(
                        documento,
                        firstNonBlank(request.razaoSocial(), destinatario.nomeExibicao()),
                        firstNonBlank(destinatario.email(), request.email()),
                        firstNonBlank(destinatario.telefone(), request.telefone()),
                        null,
                        Boolean.TRUE.equals(request.possuiContaGovBr()),
                        Boolean.TRUE.equals(request.grandeEmpresa()),
                        Boolean.TRUE.equals(request.banco()),
                        Boolean.TRUE.equals(request.fazendaPublica()),
                        !Boolean.FALSE.equals(request.cnpjAtivo())
                );
            };
        }
        return switch (destinatario.kind()) {
            case PESSOA_FISICA,
                    PARTE,
                    TERCEIRO,
                    AUXILIAR_JUSTICA -> new CitacaoIntimacaoEngine.PerfilDestinatario.PessoaFisica(
                    documento,
                    destinatario.nomeExibicao(),
                    firstNonBlank(destinatario.govbrAccountId(), request.govbrAccountId()),
                    firstNonBlank(destinatario.email(), request.email()),
                    firstNonBlank(destinatario.telefone(), request.telefone()),
                    Boolean.TRUE.equals(request.possuiContaGovBr()),
                    Boolean.TRUE.equals(request.possuiAdvogado())
            );
            case PESSOA_JURIDICA -> new CitacaoIntimacaoEngine.PerfilDestinatario.PessoaJuridica(
                    documento,
                    firstNonBlank(request.razaoSocial(), destinatario.nomeExibicao()),
                    firstNonBlank(destinatario.email(), request.email()),
                    firstNonBlank(destinatario.telefone(), request.telefone()),
                    null,
                    Boolean.TRUE.equals(request.possuiContaGovBr()),
                    Boolean.TRUE.equals(request.grandeEmpresa()),
                    Boolean.TRUE.equals(request.banco()),
                    Boolean.TRUE.equals(request.fazendaPublica()),
                    !Boolean.FALSE.equals(request.cnpjAtivo())
            );
            case ADVOGADO -> new CitacaoIntimacaoEngine.PerfilDestinatario.AdvogadoOab(
                    documento,
                    firstNonBlank(destinatario.oabNumero(), request.oabNumero()),
                    firstNonBlank(destinatario.uf(), request.uf()),
                    firstNonBlank(destinatario.email(), request.email()),
                    true,
                    "PJB"
            );
            case ORGAO_INSTITUCIONAL,
                    UNIDADE_INSTITUCIONAL -> new CitacaoIntimacaoEngine.PerfilDestinatario.PessoaJuridica(
                    documento,
                    firstNonBlank(request.razaoSocial(), destinatario.nomeExibicao()),
                    firstNonBlank(destinatario.email(), request.email()),
                    firstNonBlank(destinatario.telefone(), request.telefone()),
                    null,
                    Boolean.TRUE.equals(request.possuiContaGovBr()),
                    Boolean.TRUE.equals(request.grandeEmpresa()),
                    Boolean.TRUE.equals(request.banco()),
                    Boolean.TRUE.equals(request.fazendaPublica()),
                    !Boolean.FALSE.equals(request.cnpjAtivo())
            );
        };
    }

    private ResolucaoDestinatarioProcessualResult resolverDestinatarioProcessualInterno(NationalCommunicationDispatchRequest request,
                                                                                       Processo processo,
                                                                                       ResolucaoAtoCanonicoResult atoCanonicoPreview) {
        DestinatarioInstitucionalKind canonicalKind = atoCanonicoPreview == null || atoCanonicoPreview.politica() == null
                ? null
                : atoCanonicoPreview.politica().destinatarioKind();
        PapelProcessualInstitucional canonicalPapel = atoCanonicoPreview == null || atoCanonicoPreview.politica() == null
                ? null
                : atoCanonicoPreview.politica().papelProcessual();
        return destinatarioProcessualResolverApplicationService.resolver(new ResolucaoDestinatarioProcessualRequest(
                processo.getId(),
                processo.getNumeroProcesso(),
                request.tipoComunicacao(),
                request.destinatarioTipo(),
                request.destinatarioProcessualTipo(),
                request.destinatarioInstitucionalKind() != null ? request.destinatarioInstitucionalKind() : canonicalKind,
                request.papelProcessualInstitucional() != null ? request.papelProcessualInstitucional() : canonicalPapel,
                request.unidadeInstitucionalCodigo(),
                request.documento(),
                firstNonBlank(request.razaoSocial(), request.nome()),
                request.email(),
                request.telefone(),
                request.oabNumero(),
                request.govbrAccountId(),
                firstNonBlank(request.uf(), inferUf(processo, null)),
                firstNonBlank(request.comarca(), inferComarca(processo)),
                firstNonBlank(request.foro(), inferForo(processo)),
                request.possuiContaGovBr(),
                request.possuiAdvogado(),
                request.fazendaPublica(),
                request.intimacaoPessoalInstitucional(),
                request.tipoComunicacao().isCitacao(),
                request.tipoComunicacao().isIntimacao()
        ));
    }

    private String mergeRecipientFundamento(String fundamentoAdicional, ResolucaoDestinatarioProcessualResult destinatarioPreview) {
        if (destinatarioPreview == null) {
            return fundamentoAdicional;
        }
        String recipient = "destinatarioProcessual[kind=" + destinatarioPreview.destinatario().kind().name()
                + ";trilho=" + destinatarioPreview.trilho().name()
                + ";institucional=" + (destinatarioPreview.destinatario().destinatarioInstitucionalKind() == null ? "NA" : destinatarioPreview.destinatario().destinatarioInstitucionalKind().name())
                + ";hash=" + destinatarioPreview.hashResolucao() + "]";
        return fundamentoAdicional == null || fundamentoAdicional.isBlank() ? recipient : fundamentoAdicional + " | " + recipient;
    }

    private NationalCommunicationProcessualRecipientResolveResponse toRecipientResponse(ResolucaoDestinatarioProcessualResult result) {
        return new NationalCommunicationProcessualRecipientResolveResponse(
                result.destinatario().kind().name(),
                result.trilho().name(),
                result.destinatario().legacyKind() == null ? null : result.destinatario().legacyKind().name(),
                result.destinatario().documentoPrincipal(),
                result.destinatario().nomeExibicao(),
                result.destinatario().destinatarioInstitucionalKind() == null ? null : result.destinatario().destinatarioInstitucionalKind().name(),
                result.destinatario().papelProcessualInstitucional() == null ? null : result.destinatario().papelProcessualInstitucional().name(),
                result.destinatario().unidadeInstitucionalCodigo(),
                result.usaFluxoPessoal(),
                result.usaFluxoInstitucional(),
                result.destinatario().exigeCaixaInstitucional(),
                result.destinatario().exigeIntimacaoPessoal(),
                result.admiteCitacao(),
                result.admiteIntimacao(),
                result.hashResolucao(),
                result.justificativas()
        );
    }

    private NationalCommunicationInstitutionalResolveResponse toInstitutionalResponse(ResolucaoDestinoInstitucionalResult result) {
        return new NationalCommunicationInstitutionalResolveResponse(
                result.alvo().destinatarioKind().name(),
                result.alvo().papelProcessual().name(),
                result.alvo().unidade().codigo(),
                result.alvo().unidade().nomeOficial(),
                result.alvo().caixa().codigo(),
                result.alvo().caixa().nomeExibicao(),
                result.alvo().canalPrincipal().canal().name(),
                result.alvo().canaisElegiveis().stream().map(canal -> canal.canal().name()).distinct().toList(),
                result.alvo().unidade().tribunalCodigo(),
                result.alvo().unidade().uf(),
                result.alvo().unidade().comarca(),
                result.alvo().unidade().foro(),
                result.alvo().unidade().ramoDireito() == null ? null : result.alvo().unidade().ramoDireito().name(),
                result.alvo().unidade().grauJurisdicao() == null ? null : result.alvo().unidade().grauJurisdicao().name(),
                result.alvo().hashResolucao(),
                result.justificativas(),
                result.catalogVersion()
        );
    }

    private java.util.Optional<ResolucaoDestinoInstitucionalResult> preResolverInstitucional(NationalCommunicationDispatchRequest request,
                                                                                              Processo processo,
                                                                                              ResolucaoAtoCanonicoResult atoCanonicoPreview,
                                                                                              ResolucaoDestinatarioProcessualResult destinatarioPreview) {
        if (!destinatarioPreview.usaFluxoInstitucional() || destinatarioPreview.destinatario().destinatarioInstitucionalKind() == null) {
            return java.util.Optional.empty();
        }
        DestinatarioInstitucionalKind kind = destinatarioPreview.destinatario().destinatarioInstitucionalKind();
        PapelProcessualInstitucional papel = destinatarioPreview.destinatario().papelProcessualInstitucional() == null
                ? defaultPapel(kind)
                : destinatarioPreview.destinatario().papelProcessualInstitucional();
        String fundamento = mergeRecipientFundamento(request.fundamentoAdicional(), destinatarioPreview);
        boolean exigeCiencia = destinatarioPreview.destinatario().exigeIntimacaoPessoal();
        if (atoCanonicoPreview != null && atoCanonicoPreview.atoCanonico() != null && atoCanonicoPreview.atoCanonico() != com.tcc.pjb.backend.model.entity.enums.AtoCanonicoProcessual.NENHUM) {
            var politica = atoCanonicoPreview.politica();
            if (politica.destinatarioKind() == kind) {
                papel = politica.papelProcessual();
                fundamento = mergeCanonicalFundamento(fundamento, atoCanonicoPreview);
                exigeCiencia = politica.exigeCienciaPessoal();
            }
        }
        return java.util.Optional.of(catalogoInstitucionalUnificadoService.resolver(new ResolucaoDestinoInstitucionalRequest(
                processo.getId(),
                processo.getNumeroProcesso(),
                kind,
                papel,
                processo.getRamoDireito(),
                inferGrau(processo),
                firstNonBlank(destinatarioPreview.destinatario().uf(), inferUf(processo, request.uf())),
                firstNonBlank(destinatarioPreview.destinatario().comarca(), inferComarca(processo)),
                firstNonBlank(destinatarioPreview.destinatario().foro(), inferForo(processo)),
                request.unidadeInstitucionalCodigo(),
                null,
                fundamento,
                exigeCiencia
        )));
    }

    private String mergeFundamentoAdicional(String fundamentoAdicional,
                                            ResolucaoDestinoInstitucionalResult institucionalPreview,
                                            ResolucaoAtoCanonicoResult atoCanonicoPreview,
                                            ResolucaoRoteamentoInstitucionalResult roteamentoPreview) {
        String merged = fundamentoAdicional;
        if (atoCanonicoPreview != null) {
            merged = mergeCanonicalFundamento(merged, atoCanonicoPreview);
        }
        if (institucionalPreview != null) {
            String route = "roteamentoInstitucional[unidade=" + institucionalPreview.alvo().unidade().codigo()
                    + ";caixa=" + institucionalPreview.alvo().caixa().codigo()
                    + ";canal=" + institucionalPreview.alvo().canalPrincipal().canal().name()
                    + ";hash=" + institucionalPreview.alvo().hashResolucao() + "]";
            merged = merged == null || merged.isBlank() ? route : merged + " | " + route;
        }
        if (roteamentoPreview == null) {
            return merged;
        }
        String delivery = "politicaEntrega[canalPrincipal=" + roteamentoPreview.planoEntrega().canalPrincipal().canal().name()
                + ";fallbacks=" + roteamentoPreview.planoEntrega().canaisFallback().stream().map(canal -> canal.canal().name()).toList()
                + ";slaCiencia=" + roteamentoPreview.slaCienciaHoras()
                + ";slaResposta=" + roteamentoPreview.slaRespostaHoras()
                + ";gate=" + roteamentoPreview.gateCode()
                + ";hash=" + roteamentoPreview.hashResolucao() + "]";
        return merged == null || merged.isBlank() ? delivery : merged + " | " + delivery;
    }

    private String mergeCanonicalFundamento(String fundamentoAdicional, ResolucaoAtoCanonicoResult atoCanonicoPreview) {
        if (atoCanonicoPreview == null || atoCanonicoPreview.atoCanonico() == null) {
            return fundamentoAdicional;
        }
        String canonical = "atoCanonico[ato=" + atoCanonicoPreview.atoCanonico().name()
                + ";tipo=" + atoCanonicoPreview.politica().tipoComunicacao().name()
                + ";destinatario=" + atoCanonicoPreview.politica().destinatarioKind().name()
                + ";gate=" + atoCanonicoPreview.politica().gateCode()
                + ";hash=" + atoCanonicoPreview.hashResolucao() + "]";
        if (fundamentoAdicional == null || fundamentoAdicional.isBlank()) {
            return canonical;
        }
        return fundamentoAdicional + " | " + canonical;
    }

    private NationalCommunicationRoutingResolveResponse toRoutingResponse(ResolucaoRoteamentoInstitucionalResult result) {
        return new NationalCommunicationRoutingResolveResponse(
                result.alvo().destinatarioKind().name(),
                result.alvo().papelProcessual().name(),
                result.tipoComunicacaoEfetiva().name(),
                result.alvo().unidade().codigo(),
                result.alvo().unidade().nomeOficial(),
                result.alvo().caixa().codigo(),
                result.alvo().caixa().nomeExibicao(),
                result.planoEntrega().canalPrincipal().canal().name(),
                result.planoEntrega().canaisFallback().stream().map(canal -> canal.canal().name()).toList(),
                result.slaCienciaHoras(),
                result.slaRespostaHoras(),
                result.planoEntrega().forcarDigital(),
                result.planoEntrega().forcarOficial(),
                result.bloqueiaFluxo(),
                result.gateCode(),
                result.alvo().unidade().tribunalCodigo(),
                result.alvo().unidade().uf(),
                result.alvo().unidade().comarca(),
                result.alvo().unidade().foro(),
                result.hashResolucao(),
                result.justificativas(),
                result.catalogVersion()
        );
    }

    private java.util.Optional<ResolucaoRoteamentoInstitucionalResult> preResolverRoteamentoInstitucional(NationalCommunicationDispatchRequest request,
                                                                                                            Processo processo,
                                                                                                            ResolucaoAtoCanonicoResult atoCanonicoPreview,
                                                                                                            ResolucaoDestinatarioProcessualResult destinatarioPreview) {
        if (!destinatarioPreview.usaFluxoInstitucional() || destinatarioPreview.destinatario().destinatarioInstitucionalKind() == null) {
            return java.util.Optional.empty();
        }
        DestinatarioInstitucionalKind kind = destinatarioPreview.destinatario().destinatarioInstitucionalKind();
        PapelProcessualInstitucional papel = destinatarioPreview.destinatario().papelProcessualInstitucional() == null
                ? defaultPapel(kind)
                : destinatarioPreview.destinatario().papelProcessualInstitucional();
        boolean exigeCiencia = destinatarioPreview.destinatario().exigeIntimacaoPessoal();
        com.tcc.pjb.backend.model.entity.enums.AtoCanonicoProcessual atoCanonico = null;
        boolean bloqueioFluxo = papel.bloqueiaMarcoProcessualSensivel();
        if (atoCanonicoPreview != null && atoCanonicoPreview.atoCanonico() != null && atoCanonicoPreview.atoCanonico() != com.tcc.pjb.backend.model.entity.enums.AtoCanonicoProcessual.NENHUM) {
            var politica = atoCanonicoPreview.politica();
            if (politica.destinatarioKind() == kind) {
                papel = politica.papelProcessual();
                exigeCiencia = politica.exigeCienciaPessoal();
                atoCanonico = atoCanonicoPreview.atoCanonico();
                bloqueioFluxo = politica.bloqueiaFluxo();
            }
        }
        return java.util.Optional.of(motorRoteamentoComunicacaoInstitucional.resolver(new ResolucaoRoteamentoInstitucionalRequest(
                processo.getId(),
                processo.getNumeroProcesso(),
                kind,
                papel,
                request.tipoComunicacao(),
                atoCanonico,
                processo.getRamoDireito(),
                inferGrau(processo),
                firstNonBlank(destinatarioPreview.destinatario().uf(), inferUf(processo, request.uf())),
                firstNonBlank(destinatarioPreview.destinatario().comarca(), inferComarca(processo)),
                firstNonBlank(destinatarioPreview.destinatario().foro(), inferForo(processo)),
                request.unidadeInstitucionalCodigo(),
                null,
                mergeRecipientFundamento(request.fundamentoAdicional(), destinatarioPreview),
                exigeCiencia,
                null,
                false,
                bloqueioFluxo
        )));
    }

    private ResolucaoRoteamentoInstitucionalRequest buildRoutingRequest(NationalCommunicationRoutingResolveRequest request, Processo processo) {
        return new ResolucaoRoteamentoInstitucionalRequest(
                processo != null ? processo.getId() : request.processoId(),
                processo != null ? processo.getNumeroProcesso() : null,
                request.destinatarioKind(),
                request.papelProcessual(),
                request.tipoComunicacao(),
                request.atoCanonico(),
                request.ramoDireito() != null ? request.ramoDireito() : processo != null ? processo.getRamoDireito() : null,
                request.grauJurisdicao() != null ? request.grauJurisdicao() : inferGrau(processo),
                firstNonBlank(request.uf(), inferUf(processo, null)),
                firstNonBlank(request.comarca(), inferComarca(processo)),
                firstNonBlank(request.foro(), inferForo(processo)),
                request.unidadeSugerida(),
                request.nucleoSugerido(),
                request.fundamentoLegal(),
                Boolean.TRUE.equals(request.exigeCienciaPessoal()) || request.papelProcessual().exigeCienciaPessoalPreferencial(),
                request.canalPreferencial(),
                Boolean.TRUE.equals(request.urgente()),
                Boolean.TRUE.equals(request.bloqueioFluxoSensivel()) || request.papelProcessual().bloqueiaMarcoProcessualSensivel()
        );
    }

    private ResolucaoDestinoInstitucionalRequest buildInstitutionalRequest(NationalCommunicationInstitutionalResolveRequest request, Processo processo) {
        RamoDireito ramo = request.ramoDireito() != null ? request.ramoDireito() : processo != null ? processo.getRamoDireito() : null;
        GrauJurisdicao grau = request.grauJurisdicao() != null ? request.grauJurisdicao() : inferGrau(processo);
        return new ResolucaoDestinoInstitucionalRequest(
                processo != null ? processo.getId() : request.processoId(),
                processo != null ? processo.getNumeroProcesso() : null,
                request.destinatarioKind(),
                request.papelProcessual(),
                ramo,
                grau,
                firstNonBlank(request.uf(), inferUf(processo, null)),
                firstNonBlank(request.comarca(), inferComarca(processo)),
                firstNonBlank(request.foro(), inferForo(processo)),
                request.unidadeSugerida(),
                request.nucleoSugerido(),
                request.fundamentoLegal(),
                Boolean.TRUE.equals(request.exigeCienciaPessoal()) || request.papelProcessual().exigeCienciaPessoalPreferencial()
        );
    }

    private ResolucaoAtoCanonicoResult resolverAtoCanonicoInterno(Processo processo, NationalCommunicationCanonicalActResolveRequest request) {
        ResolucaoAtoCanonicoRequest canonicalRequest = buildCanonicalRequest(processo, request);
        return atoCanonicoProcessualResolver.resolver(canonicalRequest);
    }

    private ResolucaoAtoCanonicoRequest buildCanonicalRequest(Processo processo, NationalCommunicationCanonicalActResolveRequest request) {
        String classe = firstNonBlank(request != null ? request.classeProcessual() : null, processo != null ? processo.getClasseProcessual() : null);
        String assunto = firstNonBlank(request != null ? request.assunto() : null, processo != null ? processo.getAssunto() : null);
        String objeto = firstNonBlank(request != null ? request.objetoProcessual() : null, processo != null ? processo.getObjetoProcessual() : null);
        String pedido = firstNonBlank(request != null ? request.pedidoPrincipal() : null, processo != null ? processo.getPedidoPrincipal() : null);
        String corpus = String.join(" ", java.util.List.of(String.valueOf(classe), String.valueOf(assunto), String.valueOf(objeto), String.valueOf(pedido)));
        return new ResolucaoAtoCanonicoRequest(
                processo != null ? processo.getId() : request != null ? request.processoId() : null,
                processo != null ? processo.getNumeroProcesso() : null,
                request != null && request.ramoDireito() != null ? request.ramoDireito() : processo != null ? processo.getRamoDireito() : null,
                request != null && request.grauJurisdicao() != null ? request.grauJurisdicao() : inferGrau(processo),
                request != null && request.faseProcessual() != null ? request.faseProcessual() : processo != null ? processo.getFaseAtual() : FaseProcessual.CONHECIMENTO,
                classe,
                assunto,
                objeto,
                pedido,
                firstNonBlank(request != null ? request.uf() : null, inferUf(processo, null)),
                firstNonBlank(request != null ? request.comarca() : null, inferComarca(processo)),
                firstNonBlank(request != null ? request.foro() : null, inferForo(processo)),
                bool(request != null ? request.presencaIncapaz() : null) || containsAny(corpus, "menor", "crianca", "criança", "adolescente", "incapaz"),
                bool(request != null ? request.interesseCriancaAdolescente() : null) || containsAny(corpus, "guarda", "alimentos", "convivencia", "convivência", "adoção", "adocao", "poder familiar", "visitas"),
                bool(request != null ? request.reuPresoOuCustodiado() : null) || containsAny(corpus, "preso", "custodiado", "unidade prisional", "penitenciaria", "penitenciária"),
                bool(request != null ? request.periciaNecessaria() : null) || containsAny(corpus, "pericia", "perícia", "perito", "laudo"),
                bool(request != null ? request.estudoPsicossocialNecessario() : null) || containsAny(corpus, "psicossocial", "estudo social", "relatorio social", "relatório social", "alienacao parental", "alienação parental"),
                bool(request != null ? request.derivacaoCejusc() : null) || containsAny(corpus, "conciliacao", "conciliação", "mediacao", "mediação", "cejusc", "autocomposicao", "autocomposição"),
                bool(request != null ? request.cooperacaoJudicial() : null) || containsAny(corpus, "carta precatoria", "carta precatória", "juizo deprecado", "juízo deprecado", "cooperacao judicial", "cooperação judicial"),
                bool(request != null ? request.fazendaPublicaNoPolo() : null) || containsAny(corpus, "municipio", "município", "estado do", "uniao", "união", "inss", "fazenda publica", "fazenda pública"),
                bool(request != null ? request.demandaColetiva() : null) || containsAny(corpus, "acao civil publica", "ação civil pública", "acao coletiva", "ação coletiva", "interesse difuso", "interesse coletivo"),
                bool(request != null ? request.falenciaOuRecuperacao() : null) || containsAny(corpus, "falencia", "falência", "recuperacao judicial", "recuperação judicial", "recuperacao extrajudicial", "recuperação extrajudicial"),
                bool(request != null ? request.curadoriaEspecial() : null) || containsAny(corpus, "curadoria especial"),
                bool(request != null ? request.conselhoTutelarNecessario() : null) || containsAny(corpus, "conselho tutelar"),
                bool(request != null ? request.orgaoTecnicoConveniadoNecessario() : null) || containsAny(corpus, "creas", "cras", "caps", "órgão técnico", "orgao tecnico"),
                bool(request != null ? request.cartorioExtrajudicialNecessario() : null) || containsAny(corpus, "cartorio", "cartório", "registro civil", "averbacao", "averbação"),
                bool(request != null ? request.contadoriaJudicialNecessaria() : null) || containsAny(corpus, "contadoria", "liquidacao", "liquidação", "calculo", "cálculo"),
                bool(request != null ? request.audienciaDesignada() : null) || containsAny(corpus, "audiencia", "audiência", "sessao", "sessão", "videoconferencia", "videoconferência")
        );
    }

    private NationalCommunicationCanonicalActResolveResponse toCanonicalResponse(ResolucaoAtoCanonicoResult result) {
        return new NationalCommunicationCanonicalActResolveResponse(
                result.atoCanonico().name(),
                result.score(),
                result.politica().destinatarioKind().name(),
                result.politica().papelProcessual().name(),
                result.politica().tipoComunicacao().name(),
                result.politica().exigeCienciaPessoal(),
                result.politica().bloqueiaFluxo(),
                result.politica().gateCode(),
                result.politica().fundamentoLegal(),
                result.hashResolucao(),
                result.justificativas()
        );
    }

    private boolean bool(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private boolean containsAny(String corpus, String... terms) {
        if (corpus == null || corpus.isBlank()) {
            return false;
        }
        String normalized = corpus.toLowerCase(java.util.Locale.ROOT);
        for (String term : terms) {
            if (normalized.contains(term.toLowerCase(java.util.Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private PapelProcessualInstitucional defaultPapel(DestinatarioInstitucionalKind kind) {
        return switch (kind) {
            case MINISTERIO_PUBLICO -> PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA;
            case DEFENSORIA_PUBLICA,
                    ADVOCACIA_PUBLICA,
                    PROCURADORIA_ESTADO,
                    PROCURADORIA_MUNICIPIO,
                    AGU,
                    FAZENDA_PUBLICA -> PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE;
            case JUIZO_DEPRECADO, ORGAO_JUDICIAL_EXTERNO -> PapelProcessualInstitucional.JUIZO_COOPERANTE;
            case PERICIA_JUDICIAL, PERITO_JUDICIAL, CEJUSC -> PapelProcessualInstitucional.AUXILIAR_JUSTICA;
            case CONTADORIA_JUDICIAL, EQUIPE_PSICOSSOCIAL, ASSISTENTE_SOCIAL_JUDICIAL, ORGAO_TECNICO_CONVENIADO -> PapelProcessualInstitucional.APOIO_TECNICO;
            case DELEGACIA_POLICIA, DELEGACIA_POLICIA_CIVIL, DELEGACIA_POLICIA_FEDERAL, CONSELHO_TUTELAR -> PapelProcessualInstitucional.ORGAO_REQUISITADO;
            case POLICIA_PENAL, UNIDADE_PRISIONAL -> PapelProcessualInstitucional.UNIDADE_EXECUTORA;
            case CARTORIO_EXTRAJUDICIAL -> PapelProcessualInstitucional.DESTINATARIO_OFICIO;
            default -> throw new IllegalStateException("Destinatário institucional sem papel padrão: " + kind);
        };
    }

    private GrauJurisdicao inferGrau(Processo processo) {
        if (processo == null || processo.getJurisdicao() == null || processo.getJurisdicao().getGrau() == null) {
            return GrauJurisdicao.PRIMEIRO_GRAU;
        }
        return processo.getJurisdicao().getGrau();
    }

    private String inferUf(Processo processo, String fallback) {
        if (processo != null && processo.getJurisdicao() != null && processo.getJurisdicao().getUf() != null && !processo.getJurisdicao().getUf().isBlank()) {
            return processo.getJurisdicao().getUf();
        }
        return fallback;
    }

    private String inferComarca(Processo processo) {
        if (processo != null && processo.getJurisdicao() != null) {
            return processo.getJurisdicao().getCidade();
        }
        return null;
    }

    private String inferForo(Processo processo) {
        if (processo != null && processo.getJurisdicao() != null) {
            return processo.getJurisdicao().getForo();
        }
        return null;
    }

    private String firstNonBlank(String primary, String secondary) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return secondary;
    }

    private NationalCommunicationInstitutionalDelegationResponse toDelegationResponse(InstitutionalDelegationAssignment assignment) {
        return new NationalCommunicationInstitutionalDelegationResponse(
                assignment.assignmentId(),
                assignment.expedicaoUuid(),
                assignment.processoId(),
                assignment.unidadeCodigo(),
                assignment.caixaCodigo(),
                assignment.deleganteUsuarioId(),
                assignment.delegadoUsuarioId(),
                assignment.tipoFluxo().name(),
                assignment.capacidades().stream().map(Enum::name).sorted().toList(),
                assignment.status().name(),
                assignment.motivo(),
                assignment.inicioVigencia(),
                assignment.fimVigencia(),
                assignment.updatedAt(),
                assignment.hashIntegridade()
        );
    }

    private NationalCommunicationInstitutionalDraftResponse toDraftResponse(InstitutionalDraftManifestation draft) {
        return new NationalCommunicationInstitutionalDraftResponse(
                draft.draftId(),
                draft.expedicaoUuid(),
                draft.processoId(),
                draft.unidadeCodigo(),
                draft.caixaCodigo(),
                draft.autorUsuarioId(),
                draft.aprovadorUsuarioId(),
                draft.status().name(),
                draft.titulo(),
                draft.conteudo(),
                draft.observacoes(),
                draft.createdAt(),
                draft.submittedAt(),
                draft.reviewedAt(),
                draft.updatedAt(),
                draft.hashIntegridade()
        );
    }

    private NationalCommunicationInstitutionalAnalyticsDashboardResponse toAnalyticsResponse(InstitutionalFlowAnalyticsDashboard dashboard) {
        return new NationalCommunicationInstitutionalAnalyticsDashboardResponse(
                mapAnalytics(dashboard.falhasPorMotivo()),
                mapAnalytics(dashboard.redistribuicoesPorAtoCanonico()),
                mapAnalytics(dashboard.minutasPorStatus()),
                mapAnalytics(dashboard.delegacoesPorTipo()),
                dashboard.mediaHorasCienciaAteCumprimento(),
                dashboard.mediaHorasCienciaAtePeticao(),
                dashboard.totalDelegacoesAtivas(),
                dashboard.totalSubstituicoesAtivas(),
                dashboard.totalMinutasPendentesAprovacao(),
                dashboard.insights(),
                dashboard.generatedAt()
        );
    }

    private java.util.Map<String, NationalCommunicationInstitutionalAnalyticsBucketResponse> mapAnalytics(java.util.Map<String, InstitutionalFlowAnalyticsBucket> source) {
        java.util.LinkedHashMap<String, NationalCommunicationInstitutionalAnalyticsBucketResponse> out = new java.util.LinkedHashMap<>();
        source.forEach((key, bucket) -> out.put(key, new NationalCommunicationInstitutionalAnalyticsBucketResponse(bucket.dimensao(), bucket.valor(), bucket.total(), bucket.percentual(), bucket.mediaHoras())));
        return java.util.Collections.unmodifiableMap(out);
    }

    private NationalCommunicationInstitutionalSemanticTimelineEntryResponse toSemanticTimelineResponse(InstitutionalSemanticTimelineEntry entry) {
        return new NationalCommunicationInstitutionalSemanticTimelineEntryResponse(
                entry.eventId(),
                entry.icone(),
                entry.titulo(),
                entry.descricao(),
                entry.faseSemantica(),
                entry.occurredAt()
        );
    }

}