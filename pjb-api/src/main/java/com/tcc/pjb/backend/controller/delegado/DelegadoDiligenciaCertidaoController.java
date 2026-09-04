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
import com.tcc.pjb.backend.model.dto.profile.DiligenceAutoCertificateRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceCertificateResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceCertificateDocumentLinkRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceCertificateDocumentLinkResponse;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.profile.DiligenceCertificateEvidenceService;
import com.tcc.pjb.backend.service.profile.DiligenceOperationalCertificateService;

/**
 * Certidões automáticas de diligência e vínculo de documentos-evidência.
 * Extraído de {@link DelegadoPainelController} (recorte de F6).
 */
@RestController
@RequestMapping("/api/v1/delegado")
public class DelegadoDiligenciaCertidaoController {

    private final CapabilityRateLimiter rateLimiter;
    private final DiligenceOperationalCertificateService diligenceOperationalCertificateService;
    private final DiligenceCertificateEvidenceService diligenceCertificateEvidenceService;

    public DelegadoDiligenciaCertidaoController(CapabilityRateLimiter rateLimiter,
                                                DiligenceOperationalCertificateService diligenceOperationalCertificateService,
                                                DiligenceCertificateEvidenceService diligenceCertificateEvidenceService) {
        this.rateLimiter = rateLimiter;
        this.diligenceOperationalCertificateService = diligenceOperationalCertificateService;
        this.diligenceCertificateEvidenceService = diligenceCertificateEvidenceService;
    }

    @PostMapping("/diligencias/{diligenciaId}/certidoes/auto")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<DiligenceCertificateResponse> gerarCertidaoAutomatica(@PathVariable String diligenciaId,
                                                                                @Valid @RequestBody(required = false) DiligenceAutoCertificateRequest request,
                                                                                Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_certidao_automatica", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceOperationalCertificateService.generate(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, request));
    }

    @GetMapping("/diligencias/{diligenciaId}/certidoes")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<DiligenceCertificateResponse>> historicoCertidoes(@PathVariable String diligenciaId,
                                                                                 Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_certidao_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceOperationalCertificateService.history(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, 20));
    }

    @PostMapping("/diligencias/{diligenciaId}/certidoes/{certidaoId}/documentos/vincular")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<DiligenceCertificateDocumentLinkResponse>> vincularDocumentosCertidao(@PathVariable String diligenciaId,
                                                                                                      @PathVariable Long certidaoId,
                                                                                                      @Valid @RequestBody DiligenceCertificateDocumentLinkRequest request,
                                                                                                      Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_certidao_documentos_vincular", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(diligenceCertificateEvidenceService.bind(certidaoId, TelemetriaOperacionalCanal.DELEGADO, diligenciaId, request));
    }

    @GetMapping("/diligencias/{diligenciaId}/certidoes/{certidaoId}/documentos")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<DiligenceCertificateDocumentLinkResponse>> listarDocumentosCertidao(@PathVariable String diligenciaId,
                                                                                                    @PathVariable Long certidaoId,
                                                                                                    Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_certidao_documentos_listar", ApiVersion.V1);
        return ResponseEntity.ok(diligenceCertificateEvidenceService.list(certidaoId, TelemetriaOperacionalCanal.DELEGADO, diligenciaId));
    }

    @GetMapping("/diligencias/{diligenciaId}/certidoes/{certidaoId}/documentos/sugestoes")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<DiligenceCertificateDocumentLinkResponse>> sugerirDocumentosCertidao(@PathVariable String diligenciaId,
                                                                                                     @PathVariable Long certidaoId,
                                                                                                     Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_certidao_documentos_sugestoes", ApiVersion.V1);
        return ResponseEntity.ok(diligenceCertificateEvidenceService.suggestions(certidaoId, TelemetriaOperacionalCanal.DELEGADO, diligenciaId, 10));
    }
}
