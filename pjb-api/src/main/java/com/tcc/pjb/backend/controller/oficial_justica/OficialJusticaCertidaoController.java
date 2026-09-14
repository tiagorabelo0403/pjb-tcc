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
import com.tcc.pjb.backend.model.dto.profile.DiligenceAutoCertificateRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceCertificateResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceCertificateDocumentLinkRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceCertificateDocumentLinkResponse;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.security.operational.OperationalFunctionCredentialService;
import com.tcc.pjb.backend.service.profile.DiligenceCertificateEvidenceService;
import com.tcc.pjb.backend.service.profile.DiligenceOperationalCertificateService;

/**
 * Certidões automáticas de cumprimento de mandado e vínculo de documentos-evidência.
 * Extraído de {@link OficialJusticaCampoController} (recorte de F6).
 */
@RestController
@RequestMapping("/api/v1/oficial-justica")
public class OficialJusticaCertidaoController {

    private final CapabilityRateLimiter rateLimiter;
    private final DiligenceOperationalCertificateService diligenceOperationalCertificateService;
    private final DiligenceCertificateEvidenceService diligenceCertificateEvidenceService;
    private final OperationalFunctionCredentialService credentialService;

    public OficialJusticaCertidaoController(CapabilityRateLimiter rateLimiter,
                                            DiligenceOperationalCertificateService diligenceOperationalCertificateService,
                                            DiligenceCertificateEvidenceService diligenceCertificateEvidenceService,
                                            OperationalFunctionCredentialService credentialService) {
        this.rateLimiter = rateLimiter;
        this.diligenceOperationalCertificateService = diligenceOperationalCertificateService;
        this.diligenceCertificateEvidenceService = diligenceCertificateEvidenceService;
        this.credentialService = credentialService;
    }

    @PostMapping("/mandados/{mandadoId}/certidoes/auto")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceCertificateResponse> gerarCertidaoAutomatica(@PathVariable String mandadoId,
                                                                                @Valid @RequestBody(required = false) DiligenceAutoCertificateRequest request,
                                                                                @RequestHeader(name = OperationalFunctionCredentialService.HEADER_UNLOCK_TOKEN, required = false) String unlockToken,
                                                                                Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_certidao_automatica", ApiVersion.V1);
        credentialService.consumeUnlockTokenForCurrentUser(OperationalFunctionCredentialService.OFFICIAL_PERSONAL_SERVICE_WRITE, "OFICIAL_CERTIDAO_AUTOMATICA", mandadoId, unlockToken);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceOperationalCertificateService.generate(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, request));
    }

    @GetMapping("/mandados/{mandadoId}/certidoes")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<DiligenceCertificateResponse>> historicoCertidoes(@PathVariable String mandadoId,
                                                                                 Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_certidao_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceOperationalCertificateService.history(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, 20));
    }

    @PostMapping("/mandados/{mandadoId}/certidoes/{certidaoId}/documentos/vincular")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<DiligenceCertificateDocumentLinkResponse>> vincularDocumentosCertidao(@PathVariable String mandadoId,
                                                                                                      @PathVariable Long certidaoId,
                                                                                                      @Valid @RequestBody DiligenceCertificateDocumentLinkRequest request,
                                                                                                      Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_certidao_documentos_vincular", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(diligenceCertificateEvidenceService.bind(certidaoId, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, request));
    }

    @GetMapping("/mandados/{mandadoId}/certidoes/{certidaoId}/documentos")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<DiligenceCertificateDocumentLinkResponse>> listarDocumentosCertidao(@PathVariable String mandadoId,
                                                                                                    @PathVariable Long certidaoId,
                                                                                                    Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_certidao_documentos_listar", ApiVersion.V1);
        return ResponseEntity.ok(diligenceCertificateEvidenceService.list(certidaoId, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId));
    }

    @GetMapping("/mandados/{mandadoId}/certidoes/{certidaoId}/documentos/sugestoes")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<DiligenceCertificateDocumentLinkResponse>> sugerirDocumentosCertidao(@PathVariable String mandadoId,
                                                                                                     @PathVariable Long certidaoId,
                                                                                                     Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_certidao_documentos_sugestoes", ApiVersion.V1);
        return ResponseEntity.ok(diligenceCertificateEvidenceService.suggestions(certidaoId, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, 10));
    }
}
