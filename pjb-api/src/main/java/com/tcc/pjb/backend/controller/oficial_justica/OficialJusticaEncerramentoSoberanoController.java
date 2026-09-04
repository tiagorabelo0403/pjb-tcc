package com.tcc.pjb.backend.controller.oficial_justica;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaCumprimentoEncerramentoResponse;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaCumprimentoEncerramentoRequest;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaCumprimentoSoberanoService;
import com.tcc.pjb.backend.service.security.operational.OperationalFunctionCredentialService;

/**
 * Encerramento soberano do cumprimento de mandado — ação final protegida por token de
 * desbloqueio, distinta do encerramento operacional comum.
 * Extraído de {@link OficialJusticaCampoController} (recorte de F6).
 */
@RestController
@RequestMapping("/api/v1/oficial-justica")
public class OficialJusticaEncerramentoSoberanoController {

    private final CapabilityRateLimiter rateLimiter;
    private final OficialJusticaCumprimentoSoberanoService oficialJusticaCumprimentoSoberanoService;
    private final OperationalFunctionCredentialService credentialService;

    public OficialJusticaEncerramentoSoberanoController(CapabilityRateLimiter rateLimiter,
                                                         OficialJusticaCumprimentoSoberanoService oficialJusticaCumprimentoSoberanoService,
                                                         OperationalFunctionCredentialService credentialService) {
        this.rateLimiter = rateLimiter;
        this.oficialJusticaCumprimentoSoberanoService = oficialJusticaCumprimentoSoberanoService;
        this.credentialService = credentialService;
    }

    @PostMapping("/mandados/{mandadoId}/encerramento-soberano")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<OficialJusticaCumprimentoEncerramentoResponse> encerrarSoberanamente(@PathVariable String mandadoId,
                                                                                                @Valid @RequestBody(required = false) OficialJusticaCumprimentoEncerramentoRequest request,
                                                                                                @RequestHeader(name = OperationalFunctionCredentialService.HEADER_UNLOCK_TOKEN, required = false) String unlockToken,
                                                                                                Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_encerramento_soberano", ApiVersion.V1);
        credentialService.consumeUnlockTokenForCurrentUser(OperationalFunctionCredentialService.OFFICIAL_PERSONAL_SERVICE_WRITE, "OFICIAL_ENCERRAMENTO_SOBERANO", mandadoId, unlockToken);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(oficialJusticaCumprimentoSoberanoService.encerrar(mandadoId, request));
    }
}
