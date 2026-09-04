package com.tcc.pjb.backend.controller.oficial_justica;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.profile.DiligenceAutomaticFilingRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceAutomaticFilingResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalAnnexationRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalAnnexationResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceOperationalClosureRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceOperationalClosureResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceOperationalLinkResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceProcessFormalizationRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceProcessFormalizationResponse;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.security.operational.OperationalFunctionCredentialService;
import com.tcc.pjb.backend.service.profile.DiligenceAutomaticFilingService;
import com.tcc.pjb.backend.service.profile.DiligenceInstitutionalAnnexationService;
import com.tcc.pjb.backend.service.profile.DiligenceOperationalClosureService;
import com.tcc.pjb.backend.service.profile.DiligenceProcessFormalizationService;
import com.tcc.pjb.backend.service.profile.DiligenceReferenceResolverService;

/**
 * Desfechos processuais do cumprimento de mandado: vínculo operacional, encerramento, formalização
 * processual, juntada automática e anexação institucional.
 * Extraído de {@link OficialJusticaCampoController} (recorte de F6).
 */
@RestController
@RequestMapping("/api/v1/oficial-justica")
public class OficialJusticaDiligenciaProcessualController {

    private final CapabilityRateLimiter rateLimiter;
    private final DiligenceReferenceResolverService diligenceReferenceResolverService;
    private final DiligenceOperationalClosureService diligenceOperationalClosureService;
    private final DiligenceProcessFormalizationService diligenceProcessFormalizationService;
    private final DiligenceAutomaticFilingService diligenceAutomaticFilingService;
    private final DiligenceInstitutionalAnnexationService diligenceInstitutionalAnnexationService;
    private final OperationalFunctionCredentialService credentialService;

    public OficialJusticaDiligenciaProcessualController(CapabilityRateLimiter rateLimiter,
                                                         DiligenceReferenceResolverService diligenceReferenceResolverService,
                                                         DiligenceOperationalClosureService diligenceOperationalClosureService,
                                                         DiligenceProcessFormalizationService diligenceProcessFormalizationService,
                                                         DiligenceAutomaticFilingService diligenceAutomaticFilingService,
                                                         DiligenceInstitutionalAnnexationService diligenceInstitutionalAnnexationService,
                                                         OperationalFunctionCredentialService credentialService) {
        this.rateLimiter = rateLimiter;
        this.diligenceReferenceResolverService = diligenceReferenceResolverService;
        this.diligenceOperationalClosureService = diligenceOperationalClosureService;
        this.diligenceProcessFormalizationService = diligenceProcessFormalizationService;
        this.diligenceAutomaticFilingService = diligenceAutomaticFilingService;
        this.diligenceInstitutionalAnnexationService = diligenceInstitutionalAnnexationService;
        this.credentialService = credentialService;
    }

    @GetMapping("/mandados/{mandadoId}/vinculo-operacional")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceOperationalLinkResponse> vinculoOperacional(@PathVariable String mandadoId,
                                                                               Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_vinculo_operacional", ApiVersion.V1);
        return ResponseEntity.ok(diligenceReferenceResolverService.describe(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId));
    }

    @PostMapping("/mandados/{mandadoId}/encerramento-operacional")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceOperationalClosureResponse> encerrarOperacional(@PathVariable String mandadoId,
                                                                                   @Valid @RequestBody DiligenceOperationalClosureRequest request,
                                                                                   Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_encerramento_operacional", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceOperationalClosureService.close(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, request));
    }

    @GetMapping("/mandados/{mandadoId}/encerramentos-operacionais")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<DiligenceOperationalClosureResponse>> historicoEncerramentos(@PathVariable String mandadoId,
                                                                                             Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_encerramento_operacional_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceOperationalClosureService.history(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, 20));
    }

    @PostMapping("/mandados/{mandadoId}/formalizacao-processual")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceProcessFormalizationResponse> formalizarProcessualmente(@PathVariable String mandadoId,
                                                                                           @Valid @RequestBody(required = false) DiligenceProcessFormalizationRequest request,
                                                                                           @RequestHeader(name = OperationalFunctionCredentialService.HEADER_UNLOCK_TOKEN, required = false) String unlockToken,
                                                                                           Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_formalizacao_processual", ApiVersion.V1);
        credentialService.consumeUnlockTokenForCurrentUser(OperationalFunctionCredentialService.OFFICIAL_PERSONAL_SERVICE_WRITE, "OFICIAL_FORMALIZACAO_PROCESSUAL", mandadoId, unlockToken);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceProcessFormalizationService.formalize(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, request));
    }

    @GetMapping("/mandados/{mandadoId}/formalizacoes-processuais")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<DiligenceProcessFormalizationResponse>> historicoFormalizacoes(@PathVariable String mandadoId,
                                                                                               Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_formalizacao_processual_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceProcessFormalizationService.history(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, 20));
    }

    @PostMapping("/mandados/{mandadoId}/juntadas-automaticas")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceAutomaticFilingResponse> gerarJuntadaAutomatica(@PathVariable String mandadoId,
                                                                                   @Valid @RequestBody(required = false) DiligenceAutomaticFilingRequest request,
                                                                                   @RequestHeader(name = OperationalFunctionCredentialService.HEADER_UNLOCK_TOKEN, required = false) String unlockToken,
                                                                                   Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_juntada_automatica", ApiVersion.V1);
        credentialService.consumeUnlockTokenForCurrentUser(OperationalFunctionCredentialService.OFFICIAL_PERSONAL_SERVICE_WRITE, "OFICIAL_JUNTADA_AUTOMATICA", mandadoId, unlockToken);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceAutomaticFilingService.file(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, request));
    }

    @GetMapping("/mandados/{mandadoId}/juntadas-automaticas")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<DiligenceAutomaticFilingResponse>> historicoJuntadasAutomaticas(@PathVariable String mandadoId,
                                                                                                Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_juntada_automatica_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceAutomaticFilingService.history(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, 20));
    }

    @PostMapping("/mandados/{mandadoId}/anexacoes-institucionais")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceInstitutionalAnnexationResponse> anexarInstitucionalmente(@PathVariable String mandadoId,
                                                                                              @Valid @RequestBody(required = false) DiligenceInstitutionalAnnexationRequest request,
                                                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_anexacao_institucional", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceInstitutionalAnnexationService.annex(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, request));
    }

    @GetMapping("/mandados/{mandadoId}/anexacoes-institucionais")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<DiligenceInstitutionalAnnexationResponse>> historicoAnexacoesInstitucionais(@PathVariable String mandadoId,
                                                                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_anexacao_institucional_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceInstitutionalAnnexationService.history(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, 20));
    }
}
