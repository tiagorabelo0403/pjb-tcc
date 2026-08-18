package com.tcc.pjb.backend.modules.laiane.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import com.tcc.pjb.backend.model.dto.processual.representacao.RepresentacaoProcessualPolicyResponse;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistResponse;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoProtocolPackageResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer.*;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeProcuracao;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeTese;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.modules.laiane.service.LaianeLawyerService;
import com.tcc.pjb.backend.modules.laiane.service.LaianePeticaoAssistService;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyJsonExtractor;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;

@RestController
@RequestMapping("/api/v1/laiane/lawyer")
@Validated
@PreAuthorize("hasAuthority('ROLE_ADVOGADO')")
public class LaianeLawyerController {

    private final LaianeLawyerService service;
    private final LaianePeticaoAssistService assistService;
    private final CapabilityRateLimiter capabilityRateLimiter;
    private final ObjectMapper objectMapper;
    private final PjbTimeService timeService;

    public LaianeLawyerController(LaianeLawyerService service,
                                 LaianePeticaoAssistService assistService,
                                 CapabilityRateLimiter capabilityRateLimiter,
                                 ObjectMapper objectMapper,
                                 PjbTimeService timeService) {
        this.service = service;
        this.assistService = assistService;
        this.capabilityRateLimiter = capabilityRateLimiter;
        this.objectMapper = objectMapper;
        this.timeService = timeService;
    }

    @PostMapping("/procuracoes")
    public LaianeLawyerProcuracaoResponse createProcuracao(
            Authentication authentication,
            @RequestParam Long clienteId,
            @RequestParam(required = false) String poderesJson,
            @RequestParam(required = false) String validadeAte,
            @RequestParam(required = false) String tipoInstrumento,
            @RequestParam(required = false) Long audienciaId,
            @RequestParam(required = false) String tipoAudiencia,
            @RequestParam(required = false, defaultValue = "false") boolean contextoConsensual,
            @RequestParam(required = false, defaultValue = "false") boolean poderesEspeciaisTransigir,
            @RequestParam(required = false) String termoAudienciaReferencia,
            @RequestParam(required = false) String ataAudienciaReferencia
    ) {
        enforce(authentication, "laiane_lawyer_procuracao_create");

        java.time.LocalDateTime va = null;
        if (validadeAte != null && !validadeAte.isBlank()) {
            va = java.time.LocalDateTime.parse(validadeAte);
        }
        return mapProcuracao(service.createProcuracao(
                clienteId,
                poderesJson,
                va,
                tipoInstrumento,
                audienciaId,
                tipoAudiencia,
                contextoConsensual,
                poderesEspeciaisTransigir,
                termoAudienciaReferencia,
                ataAudienciaReferencia
        ));
    }

    @PostMapping("/processos/{processoId}/habilitacoes")
    public LaianeLawyerProcuracaoResponse solicitarHabilitacao(
            Authentication authentication,
            @PathVariable Long processoId,
            @Valid @RequestBody LaianeHabilitacaoSolicitacaoRequest req
    ) {
        enforce(authentication, "laiane_lawyer_habilitacao_solicitar");

        LaianeProcuracao proc = service.solicitarHabilitacao(
                processoId,
                req.getClienteId(),
                req.getPoderesJson(),
                req.getAnexosJson(),
                req.getMensagem(),
                req.getTipoInstrumento(),
                req.getAudienciaId(),
                req.getTipoAudiencia(),
                Boolean.TRUE.equals(req.getContextoConsensual()),
                Boolean.TRUE.equals(req.getPoderesEspeciaisTransigir()),
                req.getTermoAudienciaReferencia(),
                req.getAtaAudienciaReferencia()
        );
        return mapProcuracao(proc);
    }


    @GetMapping("/processos/{processoId}/representacao/policy")
    public RepresentacaoProcessualPolicyResponse consultarPoliticaRepresentacao(
            Authentication authentication,
            @PathVariable Long processoId,
            @RequestParam(required = false) String tipoInstrumento,
            @RequestParam(required = false) Long audienciaId,
            @RequestParam(required = false) String tipoAudiencia,
            @RequestParam(required = false, defaultValue = "false") boolean contextoConsensual,
            @RequestParam(required = false, defaultValue = "false") boolean poderesEspeciaisTransigir,
            @RequestParam(required = false) String termoAudienciaReferencia,
            @RequestParam(required = false) String ataAudienciaReferencia
    ) {
        enforce(authentication, "laiane_lawyer_procuracoes_list");
        return service.consultarPoliticaRepresentacao(
                processoId,
                tipoInstrumento,
                audienciaId,
                tipoAudiencia,
                contextoConsensual,
                poderesEspeciaisTransigir,
                termoAudienciaReferencia,
                ataAudienciaReferencia
        );
    }

    @GetMapping("/procuracoes")
    public java.util.List<LaianeLawyerProcuracaoResponse> listProcuracoes(Authentication authentication) {
        enforce(authentication, "laiane_lawyer_procuracoes_list");
        return service.listMyProcuracoes().stream().map(this::mapProcuracao).toList();
    }

    @DeleteMapping("/procuracoes/{id}")
    public void revokeProcuracao(Authentication authentication, @PathVariable Long id) {
        enforce(authentication, "laiane_lawyer_procuracao_revoke");
        service.revokeProcuracao(id);
    }

    @PostMapping("/procuracoes/{id}/substabelecer")
    public LaianeLawyerProcuracaoResponse substabelecer(Authentication authentication,
                                                         @PathVariable Long id,
                                                         @Valid @RequestBody LaianeSubstabelecimentoRequest req) {
        enforce(authentication, "laiane_lawyer_procuracao_substabelecer");
        return mapProcuracao(service.substabelecer(id, req.getAdvogadoDestinoId(), req.isComReservaDePoderes()));
    }

    @PostMapping("/teses")
    public LaianeLawyerTeseResponse createTese(
            Authentication authentication,
            @RequestParam String area,
            @RequestParam String tese,
            @RequestParam String fundamentacao,
            @RequestParam(required = false) String tagsCsv
    ) {
        enforce(authentication, "laiane_lawyer_tese_create");
        return mapTese(service.createTese(area, tese, fundamentacao, tagsCsv));
    }

    @GetMapping("/teses")
    public java.util.List<LaianeLawyerTeseResponse> listTeses(Authentication authentication) {
        enforce(authentication, "laiane_lawyer_teses_list");
        return service.listMyTeses().stream().map(this::mapTese).toList();
    }

    @PostMapping("/attachments/validate")
    public LaianeLawyerAttachmentValidationResponse validateAttachments(Authentication authentication,
                                                                        @RequestBody LaianeLawyerAttachmentValidationRequest req) {
        enforce(authentication, "laiane_lawyer_attachments_validate");
        return service.validateAttachments(req);
    }

    @PostMapping("/peticao/preflight")
    public LaianePeticaoAssistResponse preflightPeticao(Authentication authentication,
                                                        @RequestBody(required = false) LaianePeticaoAssistRequest req) {
        enforce(authentication, "laiane_lawyer_peticao_preflight");
        return assistService.preflight(req);
    }

    @PostMapping("/peticao/draft-preflight")
    public LaianePeticaoAssistResponse draftPreflightPeticao(Authentication authentication,
                                                             @RequestBody(required = false) LaianePeticaoAssistRequest req) {
        enforce(authentication, "laiane_lawyer_peticao_draft_preflight");
        return assistService.draftAndPreflight(req);
    }

    @PostMapping("/peticao/protocol-package")
    public LaianePeticaoProtocolPackageResponse createProtocolPackage(Authentication authentication,
                                                                      @RequestBody(required = false) LaianePeticaoAssistRequest req) {
        enforce(authentication, "laiane_lawyer_peticao_protocol_package");
        return assistService.createProtocolPackage(req);
    }

    @PostMapping("/deadlines/delegations")
    public LaianeDeadlineDelegationResponse createDelegation(Authentication authentication,
                                                             @Valid @RequestBody LaianeDeadlineDelegationCreateRequest req) {
        enforce(authentication, "laiane_lawyer_deadline_delegation_create");
        return service.createDelegation(req);
    }

    @GetMapping("/deadlines/delegations/sent")
    public Page<LaianeDeadlineDelegationResponse> listDelegationsSent(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        enforce(authentication, "laiane_lawyer_deadline_delegations_sent");
        return service.listDelegationsSent(normalizePage(page), normalizePageSize(size));
    }

    @GetMapping("/deadlines/delegations/received")
    public Page<LaianeDeadlineDelegationResponse> listDelegationsReceived(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        enforce(authentication, "laiane_lawyer_deadline_delegations_received");
        return service.listDelegationsReceived(normalizePage(page), normalizePageSize(size));
    }

    @PostMapping("/deadlines/delegations/{id}/accept")
    public LaianeDeadlineDelegationResponse acceptDelegation(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam(required = false) String justificativa
    ) {
        enforce(authentication, "laiane_lawyer_deadline_delegation_accept");
        return service.acceptDelegation(id, justificativa);
    }

    @PostMapping("/deadlines/delegations/{id}/complete")
    public LaianeDeadlineDelegationResponse completeDelegation(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam(required = false) String justificativa
    ) {
        enforce(authentication, "laiane_lawyer_deadline_delegation_complete");
        return service.completeDelegation(id, justificativa);
    }

    @PostMapping("/deadlines/delegations/{id}/cancel")
    public LaianeDeadlineDelegationResponse cancelDelegation(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam(required = false) String justificativa
    ) {
        enforce(authentication, "laiane_lawyer_deadline_delegation_cancel");
        return service.cancelDelegation(id, justificativa);
    }

    @PostMapping("/bundles")
    public LaianeCaseBundleResponse createBundle(Authentication authentication,
                                                 @Valid @RequestBody LaianeCaseBundleCreateRequest req) {
        enforce(authentication, "laiane_lawyer_bundle_create");
        return service.createBundle(req);
    }

    @GetMapping("/bundles")
    public Page<LaianeCaseBundleResponse> listBundles(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        enforce(authentication, "laiane_lawyer_bundles_list");
        return service.listBundles(status, normalizePage(page), normalizePageSize(size));
    }

    @PutMapping("/bundles/{id}")
    public LaianeCaseBundleResponse updateBundle(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody LaianeCaseBundleUpdateRequest req
    ) {
        enforce(authentication, "laiane_lawyer_bundle_update");
        return service.updateBundle(id, req);
    }

    @PostMapping("/bundles/{id}/consolidate")
    public LaianeCaseBundleConsolidationResponse consolidateBundle(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam(required = false) String justificativa
    ) {
        enforce(authentication, "laiane_lawyer_bundle_consolidate");
        return service.consolidateBundle(id, justificativa);
    }

    private java.util.Map<String, Object> extractRepresentacaoPolicy(String poderes) {
        return RepresentacaoProcessualPolicyJsonExtractor.extract(objectMapper, poderes);
    }

    private void enforce(Authentication authentication, String capability) {
        if (capabilityRateLimiter != null) {
            capabilityRateLimiter.enforce(CapabilityRateLimitDomain.LAWYER, authentication, capability, ApiVersion.V1);
        }
    }

    private LaianeLawyerTeseResponse mapTese(LaianeTese t) {
        return LaianeLawyerTeseResponse.builder()
                .id(t.getId())
                .area(t.getArea())
                .titulo(t.getTitulo())
                .corpo(t.getCorpo())
                .tagsJson(t.getTagsJson())
                .createdAt(toOffset(t.getCreatedAt()))
                .updatedAt(toOffset(t.getUpdatedAt()))
                .build();
    }

    private int normalizePage(int page) {
        return Math.max(0, page);
    }

    private int normalizePageSize(int size) {
        int normalized = size <= 0 ? 30 : size;
        return Math.min(normalized, 100);
    }

    private LaianeLawyerProcuracaoResponse mapProcuracao(LaianeProcuracao p) {
        return LaianeLawyerProcuracaoResponse.builder()
                .id(p.getId())
                .clienteId(p.getClienteId())
                .status(p.getStatus())
                .processoId(p.getProcessoId())
                .inicioVigencia(p.getInicioVigencia())
                .fimVigencia(p.getFimVigencia())
                .poderes(p.getPoderes())
                .anexosJson(p.getAnexosJson())
                .representacaoPolicy(extractRepresentacaoPolicy(p.getPoderes()))
                .createdAt(toOffset(p.getCreatedAt()))
                .updatedAt(toOffset(p.getUpdatedAt()))
                .substabelecidoDeId(p.getSubstabelecidoDe() == null ? null : p.getSubstabelecidoDe().getId())
                .comReservaDePoderes(p.isComReservaDePoderes())
                .build();
    }

    private OffsetDateTime toOffset(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atZone(timeService.legalZone()).toOffsetDateTime();
    }
}
