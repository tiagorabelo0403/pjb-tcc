package com.tcc.pjb.backend.controller.security;

import com.tcc.pjb.backend.model.dto.security.ChallengeVerifyRequest;
import com.tcc.pjb.backend.model.dto.security.SecurityChallengeVerificationResponse;
import com.tcc.pjb.backend.service.security.surface.SecuritySurfaceFacadeService;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/v1/security/challenges")
public class SecurityChallengeController {

    private final SecuritySurfaceFacadeService facadeService;

    public SecurityChallengeController(SecuritySurfaceFacadeService facadeService) {
        this.facadeService = Objects.requireNonNull(facadeService);
    }

    @PostMapping("/{challengeId}/verify")
    public ResponseEntity<SecurityChallengeVerificationResponse> verifyOtp(@PathVariable Long challengeId,
                                                                           @Valid @RequestBody ChallengeVerifyRequest request) {
        SecurityChallengeVerificationResponse response = facadeService.verifyChallenge(challengeId, request);
        return ResponseEntity.status(facadeService.challengeVerifyHttpStatus(response)).body(response);
    }
}
