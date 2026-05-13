package com.tcc.pjb.backend.controller.processual.comunicacao.flow;

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
import com.tcc.pjb.backend.service.processual.comunicacao.flow.NationalCommunicationFlowRoutes;
import com.tcc.pjb.backend.service.processual.comunicacao.flow.NationalCommunicationFlowService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(NationalCommunicationFlowRoutes.CANONICAL_BASE)
public class NationalCommunicationFlowController {

    private final NationalCommunicationFlowService service;

    public NationalCommunicationFlowController(NationalCommunicationFlowService service) {
        this.service = service;
    }

    @PostMapping(NationalCommunicationFlowRoutes.PATH_DISPATCH)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationDispatchResponse> expedir(@Valid @RequestBody NationalCommunicationDispatchRequest request) {
        return ResponseEntity.ok(service.expedir(request));
    }

    @PostMapping(NationalCommunicationFlowRoutes.PATH_ACKNOWLEDGE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> acusarCiencia(@Valid @RequestBody NationalCommunicationAcknowledgeRequest request) {
        service.acusarCiencia(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping(NationalCommunicationFlowRoutes.PATH_RESOLVE_CANONICAL_ACT)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationCanonicalActResolveResponse> resolverAtoCanonico(@RequestBody NationalCommunicationCanonicalActResolveRequest request) {
        return ResponseEntity.ok(service.resolverAtoCanonico(request));
    }

    @PostMapping(NationalCommunicationFlowRoutes.PATH_RESOLVE_PROCESSUAL_RECIPIENT)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationProcessualRecipientResolveResponse> resolverDestinatarioProcessual(@RequestBody NationalCommunicationProcessualRecipientResolveRequest request) {
        return ResponseEntity.ok(service.resolverDestinatarioProcessual(request));
    }

    @PostMapping(NationalCommunicationFlowRoutes.PATH_RESOLVE_INSTITUTIONAL)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalResolveResponse> resolverInstitucional(@Valid @RequestBody NationalCommunicationInstitutionalResolveRequest request) {
        return ResponseEntity.ok(service.resolverDestinoInstitucional(request));
    }

    @PostMapping(NationalCommunicationFlowRoutes.PATH_RESOLVE_INSTITUTIONAL_ROUTING)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationRoutingResolveResponse> resolverRoteamentoInstitucional(@Valid @RequestBody NationalCommunicationRoutingResolveRequest request) {
        return ResponseEntity.ok(service.resolverRoteamentoInstitucional(request));
    }

    @GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_BOXES)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<NationalCommunicationInstitutionalMembershipResponse>> minhasCaixasInstitucionais(@RequestParam(required = false) DestinatarioInstitucionalKind destinatarioKind,
                                                                                                                            @RequestParam(required = false) String uf,
                                                                                                                            @RequestParam(required = false) String comarca) {
        return ResponseEntity.ok(service.minhasCaixasInstitucionais(destinatarioKind, uf, comarca));
    }

    @PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_AUTHORIZE_BOX)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalAccessCheckResponse> autorizarCaixaInstitucional(@Valid @RequestBody NationalCommunicationInstitutionalAccessCheckRequest request) {
        return ResponseEntity.ok(service.autorizarCaixaInstitucional(request));
    }

    @GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_INBOX)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<NationalCommunicationInstitutionalInboxItemResponse>> listarInboxInstitucional(@RequestParam(required = false) StatusComunicacaoInstitucional status,
                                                                                                                         @RequestParam(required = false) Long processoId) {
        return ResponseEntity.ok(service.listarInboxInstitucional(status, processoId));
    }

    @PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_RECEIVE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalActionResponse> receberInboxInstitucional(@Valid @RequestBody NationalCommunicationInstitutionalReceiveRequest request) {
        return ResponseEntity.ok(service.receberInboxInstitucional(request));
    }

    @PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_REDISTRIBUTE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalActionResponse> redistribuirInboxInstitucional(@Valid @RequestBody NationalCommunicationInstitutionalRedistributeRequest request) {
        return ResponseEntity.ok(service.redistribuirInboxInstitucional(request));
    }

    @PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_CERTIFY_SCIENCE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalActionResponse> certificarCienciaInstitucional(@Valid @RequestBody NationalCommunicationInstitutionalScienceRequest request) {
        return ResponseEntity.ok(service.certificarCienciaInstitucional(request));
    }

    @PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_FULFILL)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalActionResponse> cumprirInboxInstitucional(@Valid @RequestBody NationalCommunicationInstitutionalFulfillRequest request) {
        return ResponseEntity.ok(service.cumprirInboxInstitucional(request));
    }

    @GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_TIMELINE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<NationalCommunicationInstitutionalTimelineEventResponse>> timelineInstitucional(@RequestParam String expedicaoUuid) {
        return ResponseEntity.ok(service.timelineInstitucional(expedicaoUuid));
    }

    @GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_SEMANTIC_TIMELINE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<NationalCommunicationInstitutionalSemanticTimelineEntryResponse>> timelineSemanticaInstitucional(@RequestParam String expedicaoUuid) {
        return ResponseEntity.ok(service.timelineSemanticaInstitucional(expedicaoUuid));
    }

    @GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_PROOFS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<NationalCommunicationInstitutionalDeliveryProofResponse>> provasInstitucionais(@RequestParam String expedicaoUuid) {
        return ResponseEntity.ok(service.provasInstitucionais(expedicaoUuid));
    }

    @GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_GATES)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<NationalCommunicationInstitutionalGateStateResponse>> gatesInstitucionais(@RequestParam(required = false) Long processoId,
                                                                                                                     @RequestParam(required = false) String expedicaoUuid) {
        return ResponseEntity.ok(service.gatesInstitucionais(processoId, expedicaoUuid));
    }

    @GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_DELIVERIES)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<NationalCommunicationInstitutionalDeliveryQueueItemResponse>> listarEntregasInstitucionais(@RequestParam(required = false) Long processoId,
                                                                                                                                      @RequestParam(required = false) String expedicaoUuid) {
        return ResponseEntity.ok(service.listarEntregasInstitucionais(processoId, expedicaoUuid));
    }

    @GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_DLQ)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<NationalCommunicationInstitutionalDeadLetterResponse>> listarDlqInstitucional(@RequestParam(required = false) Long processoId,
                                                                                                                         @RequestParam(required = false) String expedicaoUuid) {
        return ResponseEntity.ok(service.listarDlqInstitucional(processoId, expedicaoUuid));
    }

    @PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_REPROCESS_DELIVERY)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalDeliveryQueueItemResponse> reprocessarEntregaInstitucional(@Valid @RequestBody NationalCommunicationInstitutionalReprocessDeliveryRequest request) {
        return ResponseEntity.ok(service.reprocessarEntregaInstitucional(request));
    }

    @GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_EXTERNAL_INTEGRATIONS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<NationalCommunicationInstitutionalExternalDispatchResponse>> listarIntegracoesExternas(@RequestParam(required = false) Long processoId,
                                                                                                                               @RequestParam(required = false) String expedicaoUuid) {
        return ResponseEntity.ok(service.listarIntegracoesExternas(processoId, expedicaoUuid));
    }

    @GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_OBSERVABILITY)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalObservabilityDashboardResponse> observabilidadeInstitucional(@RequestParam(required = false) Long processoId,
                                                                                                                         @RequestParam(required = false) String uf,
                                                                                                                         @RequestParam(required = false) DestinatarioInstitucionalKind destinatarioKind) {
        return ResponseEntity.ok(service.observabilidadeInstitucional(processoId, uf, destinatarioKind));
    }

    @GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_ANALYTICS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalAnalyticsDashboardResponse> analyticsInstitucional(@RequestParam(required = false) Long processoId,
                                                                                                                 @RequestParam(required = false) String expedicaoUuid) {
        return ResponseEntity.ok(service.analyticsInstitucional(processoId, expedicaoUuid));
    }

    @GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_HARDENING)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalHardeningReportResponse> hardeningInstitucional() {
        return ResponseEntity.ok(service.hardeningInstitucional());
    }

    @PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_DELEGATE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalDelegationResponse> delegarInstitucional(@Valid @RequestBody NationalCommunicationInstitutionalDelegateRequest request) {
        return ResponseEntity.ok(service.delegarInstitucional(request));
    }

    @PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_SUBSTITUTE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalDelegationResponse> substituirInstitucional(@Valid @RequestBody NationalCommunicationInstitutionalSubstitutionRequest request) {
        return ResponseEntity.ok(service.substituirInstitucional(request));
    }

    @GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_DELEGATIONS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<NationalCommunicationInstitutionalDelegationResponse>> listarDelegacoesInstitucionais(@RequestParam String expedicaoUuid) {
        return ResponseEntity.ok(service.listarDelegacoesInstitucionais(expedicaoUuid));
    }

    @PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_DRAFT_CREATE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalDraftResponse> criarMinutaInstitucional(@Valid @RequestBody NationalCommunicationInstitutionalDraftCreateRequest request) {
        return ResponseEntity.ok(service.criarOuAtualizarMinutaInstitucional(request));
    }

    @PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_DRAFT_SUBMIT)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalDraftResponse> submeterMinutaInstitucional(@Valid @RequestBody NationalCommunicationInstitutionalDraftSubmitRequest request) {
        return ResponseEntity.ok(service.submeterMinutaInstitucional(request));
    }

    @PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_DRAFT_APPROVE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalDraftResponse> aprovarMinutaInstitucional(@Valid @RequestBody NationalCommunicationInstitutionalDraftReviewRequest request) {
        return ResponseEntity.ok(service.aprovarMinutaInstitucional(request));
    }

    @PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_DRAFT_REJECT)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalDraftResponse> rejeitarMinutaInstitucional(@Valid @RequestBody NationalCommunicationInstitutionalDraftReviewRequest request) {
        return ResponseEntity.ok(service.rejeitarMinutaInstitucional(request));
    }

    @GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_DRAFTS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<NationalCommunicationInstitutionalDraftResponse>> listarMinutasInstitucionais(@RequestParam String expedicaoUuid) {
        return ResponseEntity.ok(service.listarMinutasInstitucionais(expedicaoUuid));
    }

    @PostMapping(NationalCommunicationFlowRoutes.PATH_FALLBACK)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationDispatchResponse> fallback(@Valid @RequestBody NationalCommunicationFallbackRequest request) {
        return ResponseEntity.ok(service.acionarFallback(request));
    }

    @GetMapping(NationalCommunicationFlowRoutes.PATH_DASHBOARD)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationDashboardResponse> painel() {
        return ResponseEntity.ok(service.painel());
    }
}