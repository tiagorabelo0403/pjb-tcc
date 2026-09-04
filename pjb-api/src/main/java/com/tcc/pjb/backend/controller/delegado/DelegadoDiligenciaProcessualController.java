package com.tcc.pjb.backend.controller.delegado;

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
import com.tcc.pjb.backend.service.profile.DiligenceAutomaticFilingService;
import com.tcc.pjb.backend.service.profile.DiligenceInstitutionalAnnexationService;
import com.tcc.pjb.backend.service.profile.DiligenceOperationalClosureService;
import com.tcc.pjb.backend.service.profile.DiligenceProcessFormalizationService;
import com.tcc.pjb.backend.service.profile.DiligenceReferenceResolverService;

/**
 * Desfechos processuais de uma diligência: vínculo com o processo, encerramento operacional,
 * formalização processual, juntada automática e anexação institucional.
 * Extraído de {@link DelegadoPainelController} (recorte de F6).
 */
@RestController
@RequestMapping("/api/v1/delegado")
public class DelegadoDiligenciaProcessualController {

    private final CapabilityRateLimiter rateLimiter;
    private final DiligenceReferenceResolverService diligenceReferenceResolverService;
    private final DiligenceOperationalClosureService diligenceOperationalClosureService;
    private final DiligenceProcessFormalizationService diligenceProcessFormalizationService;
    private final DiligenceAutomaticFilingService diligenceAutomaticFilingService;
    private final DiligenceInstitutionalAnnexationService diligenceInstitutionalAnnexationService;

    public DelegadoDiligenciaProcessualController(CapabilityRateLimiter rateLimiter,
                                                   DiligenceReferenceResolverService diligenceReferenceResolverService,
                                                   DiligenceOperationalClosureService diligenceOperationalClosureService,
                                                   DiligenceProcessFormalizationService diligenceProcessFormalizationService,
                                                   DiligenceAutomaticFilingService diligenceAutomaticFilingService,
                                                   DiligenceInstitutionalAnnexationService diligenceInstitutionalAnnexationService) {
        this.rateLimiter = rateLimiter;
        this.diligenceReferenceResolverService = diligenceReferenceResolverService;
        this.diligenceOperationalClosureService = diligenceOperationalClosureService;
        this.diligenceProcessFormalizationService = diligenceProcessFormalizationService;
        this.diligenceAutomaticFilingService = diligenceAutomaticFilingService;
        this.diligenceInstitutionalAnnexationService = diligenceInstitutionalAnnexationService;
    }

    @GetMapping("/diligencias/{diligenciaId}/vinculo-operacional")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceOperationalLinkResponse> vinculoOperacional(@PathVariable String diligenciaId,
                                                                               Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_vinculo_operacional", ApiVersion.V1);
        return ResponseEntity.ok(diligenceReferenceResolverService.describe(TelemetriaOperacionalCanal.DELEGADO, diligenciaId));
    }

    @PostMapping("/diligencias/{diligenciaId}/encerramento-operacional")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceOperationalClosureResponse> encerrarOperacional(@PathVariable String diligenciaId,
                                                                                   @Valid @RequestBody DiligenceOperationalClosureRequest request,
                                                                                   Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_encerramento_operacional", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceOperationalClosureService.close(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, request));
    }

    @GetMapping("/diligencias/{diligenciaId}/encerramentos-operacionais")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<DiligenceOperationalClosureResponse>> historicoEncerramentos(@PathVariable String diligenciaId,
                                                                                             Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_encerramento_operacional_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceOperationalClosureService.history(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, 20));
    }

    @PostMapping("/diligencias/{diligenciaId}/formalizacao-processual")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceProcessFormalizationResponse> formalizarProcessualmente(@PathVariable String diligenciaId,
                                                                                           @Valid @RequestBody(required = false) DiligenceProcessFormalizationRequest request,
                                                                                           Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_formalizacao_processual", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceProcessFormalizationService.formalize(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, request));
    }

    @GetMapping("/diligencias/{diligenciaId}/formalizacoes-processuais")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<DiligenceProcessFormalizationResponse>> historicoFormalizacoes(@PathVariable String diligenciaId,
                                                                                               Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_formalizacao_processual_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceProcessFormalizationService.history(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, 20));
    }

    @PostMapping("/diligencias/{diligenciaId}/juntadas-automaticas")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceAutomaticFilingResponse> gerarJuntadaAutomatica(@PathVariable String diligenciaId,
                                                                                   @Valid @RequestBody(required = false) DiligenceAutomaticFilingRequest request,
                                                                                   Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_juntada_automatica", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceAutomaticFilingService.file(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, request));
    }

    @GetMapping("/diligencias/{diligenciaId}/juntadas-automaticas")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<DiligenceAutomaticFilingResponse>> historicoJuntadasAutomaticas(@PathVariable String diligenciaId,
                                                                                                Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_juntada_automatica_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceAutomaticFilingService.history(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, 20));
    }

    @PostMapping("/diligencias/{diligenciaId}/anexacoes-institucionais")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceInstitutionalAnnexationResponse> anexarInstitucionalmente(@PathVariable String diligenciaId,
                                                                                              @Valid @RequestBody(required = false) DiligenceInstitutionalAnnexationRequest request,
                                                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_anexacao_institucional", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceInstitutionalAnnexationService.annex(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, request));
    }

    @GetMapping("/diligencias/{diligenciaId}/anexacoes-institucionais")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<DiligenceInstitutionalAnnexationResponse>> historicoAnexacoesInstitucionais(@PathVariable String diligenciaId,
                                                                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_anexacao_institucional_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceInstitutionalAnnexationService.history(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, 20));
    }
}
