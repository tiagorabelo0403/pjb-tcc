package com.tcc.pjb.backend.controller.security;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.model.dto.security.WebAuthnEnrollFinishRequest;
import com.tcc.pjb.backend.model.dto.security.WebAuthnEnrollmentChallengeResponse;
import com.tcc.pjb.backend.model.dto.security.WebAuthnEnrollmentFinishResponse;
import com.tcc.pjb.backend.service.security.surface.SecuritySurfaceFacadeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/security/webauthn")
@PreAuthorize("isAuthenticated()")
public class WebAuthnController {

    private final SecuritySurfaceFacadeService facadeService;

    public WebAuthnController(SecuritySurfaceFacadeService facadeService) {
        this.facadeService = Objects.requireNonNull(facadeService);
    }

    @PostMapping("/enroll/options")
    public ResponseEntity<WebAuthnEnrollmentChallengeResponse> startEnroll() {
        return ResponseEntity.ok(facadeService.startEnrollment());
    }

    @PostMapping("/enroll/finish")
    public ResponseEntity<WebAuthnEnrollmentFinishResponse> finishEnroll(@Valid @RequestBody WebAuthnEnrollFinishRequest request,
                                                                          HttpServletRequest httpRequest) {
        return ResponseEntity.ok(facadeService.finishEnrollment(request, httpRequest));
    }
}
