package com.tcc.pjb.backend.controller.processo;

import com.tcc.pjb.backend.model.dto.security.SigiloZkChallengeRequest;
import com.tcc.pjb.backend.model.dto.security.SigiloZkChallengeResponse;
import com.tcc.pjb.backend.model.dto.security.SigiloZkVerificationRequest;
import com.tcc.pjb.backend.model.dto.security.SigiloZkVerificationResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.api.surface.MarketplaceSurfaceFacadeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processos/sigilo/zk")
public class SigiloZeroKnowledgeProofController {

    private final MarketplaceSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public SigiloZeroKnowledgeProofController(MarketplaceSurfaceFacadeService facadeService,
                                              CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/{processoId}/challenge")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SigiloZkChallengeResponse> challenge(@PathVariable Long processoId,
                                                               @Valid @RequestBody SigiloZkChallengeRequest request,
                                                               Authentication authentication) {
        enforce(authentication, "sigilo_zk_emitir_desafio");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.emitirSigiloChallenge(processoId, request));
    }

    @PostMapping("/{challengeId}/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SigiloZkVerificationResponse> verify(@PathVariable String challengeId,
                                                               @Valid @RequestBody SigiloZkVerificationRequest request,
                                                               Authentication authentication) {
        enforce(authentication, "sigilo_zk_verificar");
        return ResponseEntity.ok(facadeService.verificarSigiloChallenge(challengeId, request));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
