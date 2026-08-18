package com.tcc.pjb.backend.controller.processual.peticionamento;

import com.tcc.pjb.backend.model.dto.advogado.LaianePeticaoInicialEstruturarRequest;
import com.tcc.pjb.backend.model.dto.advogado.LaianePeticaoInicialProtocolarRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoEnderecoRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoEnderecoResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.journey.PeticionamentoJourneyIntelligenceResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.studio.PeticionamentoStudioDraftDiffResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.studio.PeticionamentoStudioGovernedReviewResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.studio.PeticionamentoStudioQuickDraftResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.studio.PeticionamentoStudioWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.journey.PeticionamentoSimpleProtocolWizardResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.advogado.LaianePeticaoInicialDraftRequestMapper;
import com.tcc.pjb.backend.service.advogado.LaianePeticaoInicialDraftService;
import com.tcc.pjb.backend.service.processual.peticionamento.PeticionamentoSessaoFacadeService;
import com.tcc.pjb.backend.service.processual.peticionamento.journey.PeticionamentoJourneyIntelligenceService;
import com.tcc.pjb.backend.service.processual.peticionamento.journey.PeticionamentoSimpleProtocolWizardService;
import com.tcc.pjb.backend.service.processual.peticionamento.studio.PeticionamentoStudioWorkspaceService;
import jakarta.validation.Valid;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/peticionamento")
@PreAuthorize("isAuthenticated()")
public class PeticionamentoController {

    private final PeticionamentoSessaoFacadeService facadeService;
    private final LaianePeticaoInicialDraftService draftService;
    private final PeticionamentoStudioWorkspaceService studioWorkspaceService;
    private final PeticionamentoSimpleProtocolWizardService simpleProtocolWizardService;
    private final PeticionamentoJourneyIntelligenceService journeyIntelligenceService;
    private final CapabilityRateLimiter rateLimiter;

    public PeticionamentoController(PeticionamentoSessaoFacadeService facadeService,
                                    LaianePeticaoInicialDraftService draftService,
                                    PeticionamentoStudioWorkspaceService studioWorkspaceService,
                                    PeticionamentoSimpleProtocolWizardService simpleProtocolWizardService,
                                    PeticionamentoJourneyIntelligenceService journeyIntelligenceService,
                                    CapabilityRateLimiter rateLimiter) {
        this.facadeService = Objects.requireNonNull(facadeService, "facadeService");
        this.draftService = Objects.requireNonNull(draftService, "draftService");
        this.studioWorkspaceService = Objects.requireNonNull(studioWorkspaceService, "studioWorkspaceService");
        this.simpleProtocolWizardService = Objects.requireNonNull(simpleProtocolWizardService, "simpleProtocolWizardService");
        this.journeyIntelligenceService = Objects.requireNonNull(journeyIntelligenceService, "journeyIntelligenceService");
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter");
    }

    @PostMapping("/inicial/sessao")
    public ResponseEntity<PeticionamentoSessaoResponse> abrirSessaoInicial(Authentication authentication,
                                                                           @Valid @RequestBody PeticionamentoSessaoRequest request) {
        enforce(authentication, "peticionamento_sessao_inicial");
        return ResponseEntity.ok(facadeService.abrirSessaoInicial(request));
    }

    @PostMapping("/studio/workspace")
    public ResponseEntity<PeticionamentoStudioWorkspaceResponse> abrirWorkspaceStudio(Authentication authentication,
                                                                                       @Valid @RequestBody PeticionamentoSessaoRequest request) {
        enforce(authentication, "peticionamento_studio_workspace");
        return ResponseEntity.ok(studioWorkspaceService.buildWorkspace(request));
    }

    @PostMapping("/studio/minuta-rapida")
    public ResponseEntity<PeticionamentoStudioQuickDraftResponse> gerarMinutaRapida(Authentication authentication,
                                                                                     @Valid @RequestBody PeticionamentoSessaoRequest request) {
        enforce(authentication, "peticionamento_studio_minuta_rapida");
        return ResponseEntity.ok(studioWorkspaceService.buildQuickDraft(request));
    }


    @PostMapping("/studio/revisao-governada")
    public ResponseEntity<PeticionamentoStudioGovernedReviewResponse> revisarGovernado(Authentication authentication,
                                                                                       @Valid @RequestBody PeticionamentoSessaoRequest request) {
        enforce(authentication, "peticionamento_studio_revisao_governada");
        return ResponseEntity.ok(studioWorkspaceService.buildGovernedReview(request));
    }

    @PostMapping("/studio/diff-minuta")
    public ResponseEntity<PeticionamentoStudioDraftDiffResponse> compararMinuta(Authentication authentication,
                                                                                @Valid @RequestBody PeticionamentoSessaoRequest request) {
        enforce(authentication, "peticionamento_studio_diff_minuta");
        return ResponseEntity.ok(studioWorkspaceService.buildDraftDiff(request));
    }


    @PostMapping("/studio/wizard-protocolo-simples")
    public ResponseEntity<PeticionamentoSimpleProtocolWizardResponse> wizardProtocoloSimples(Authentication authentication,
                                                                                              @Valid @RequestBody PeticionamentoSessaoRequest request) {
        enforce(authentication, "peticionamento_studio_wizard_protocolo_simples");
        return ResponseEntity.ok(simpleProtocolWizardService.build(request));
    }


    @PostMapping("/studio/jornada-inteligente")
    public ResponseEntity<PeticionamentoJourneyIntelligenceResponse> jornadaInteligente(Authentication authentication,
                                                                                         @Valid @RequestBody PeticionamentoSessaoRequest request) {
        enforce(authentication, "peticionamento_studio_jornada_inteligente");
        return ResponseEntity.ok(journeyIntelligenceService.analyze(request));
    }

    @PostMapping("/enderecos/cep")
    public ResponseEntity<PeticionamentoEnderecoResponse> resolverCep(Authentication authentication,
                                                                      @RequestBody(required = false) PeticionamentoEnderecoRequest request) {
        enforce(authentication, "peticionamento_resolver_cep");
        return ResponseEntity.ok(facadeService.resolverEndereco(request));
    }

    @PostMapping("/inicial/rascunhos/estruturar")
    public ResponseEntity<LaianePeticaoInicialDraftService.DraftView> estruturarRascunho(Authentication authentication,
                                                                                          @Valid @RequestBody LaianePeticaoInicialEstruturarRequest request) {
        enforce(authentication, "peticionamento_rascunho_estruturar");
        return ResponseEntity.ok(draftService.estruturar(LaianePeticaoInicialDraftRequestMapper.toServiceRequest(request)));
    }

    @PostMapping("/inicial/rascunhos/salvar")
    public ResponseEntity<LaianePeticaoInicialDraftService.DraftView> salvarRascunho(Authentication authentication,
                                                                                      @Valid @RequestBody LaianePeticaoInicialEstruturarRequest request) {
        enforce(authentication, "peticionamento_rascunho_salvar");
        return ResponseEntity.ok(draftService.salvar(LaianePeticaoInicialDraftRequestMapper.toServiceRequest(request)));
    }

    @GetMapping("/inicial/rascunhos/minhas")
    public ResponseEntity<java.util.List<LaianePeticaoInicialDraftService.DraftView>> listarMeusRascunhos(Authentication authentication) {
        enforce(authentication, "peticionamento_rascunho_listar");
        return ResponseEntity.ok(draftService.listarMinhas());
    }

    @GetMapping("/inicial/rascunhos/{draftId}")
    public ResponseEntity<LaianePeticaoInicialDraftService.DraftView> detalharRascunho(Authentication authentication,
                                                                                        @PathVariable Long draftId) {
        enforce(authentication, "peticionamento_rascunho_detalhar");
        return ResponseEntity.ok(draftService.detalhar(draftId));
    }

    @PostMapping("/inicial/rascunhos/{draftId}/protocolar")
    public ResponseEntity<LaianePeticaoInicialDraftService.ProtocolarResult> protocolarRascunho(Authentication authentication,
                                                                                                 @PathVariable Long draftId,
                                                                                                 @RequestBody(required = false) LaianePeticaoInicialProtocolarRequest request) {
        enforce(authentication, "peticionamento_rascunho_protocolar");
        return ResponseEntity.ok(draftService.protocolar(draftId, request == null ? null :
                new LaianePeticaoInicialDraftService.ProtocolarRequest(
                        request.tipoJustica(),
                        request.tipoPartePrincipal(),
                        request.condicoesAplicaveis(),
                        request.documentosAnexados())));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(resolveDomain(authentication), authentication, capability, ApiVersion.V1);
    }

    private CapabilityRateLimitDomain resolveDomain(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return CapabilityRateLimitDomain.LAWYER;
        }
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(authority -> authority == null ? null : authority.getAuthority())
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        boolean institutional = authorities.stream()
                .anyMatch(authority -> authority.contains("DEFENSOR")
                        || authority.contains("PROCURADOR")
                        || authority.contains("PROMOTOR")
                        || authority.contains("MINISTERIO_PUBLICO")
                        || authority.contains("PROCURADORIA")
                        || authority.contains("DEFENSORIA"));
        if (institutional) {
            return CapabilityRateLimitDomain.INSTITUCIONAL;
        }
        if (authorities.contains("ROLE_CIDADAO")) {
            return CapabilityRateLimitDomain.CITIZEN;
        }
        return CapabilityRateLimitDomain.LAWYER;
    }
}
