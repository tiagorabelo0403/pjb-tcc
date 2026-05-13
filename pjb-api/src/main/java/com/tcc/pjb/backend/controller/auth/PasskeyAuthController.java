package com.tcc.pjb.backend.controller.auth;

import com.tcc.pjb.backend.model.dto.security.PasskeyFinishRequest;
import com.tcc.pjb.backend.model.dto.security.PasskeyStartRequest;
import com.tcc.pjb.backend.model.dto.security.WebAuthnAuthenticationResponse;
import com.tcc.pjb.backend.model.dto.security.WebAuthnChallengeResponse;
import com.tcc.pjb.backend.service.auth.surface.WebAuthnSurfaceFacadeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/passkey")
@PreAuthorize("permitAll()")
public class PasskeyAuthController {

    private final WebAuthnSurfaceFacadeService facadeService;

    public PasskeyAuthController(WebAuthnSurfaceFacadeService facadeService) {
        this.facadeService = Objects.requireNonNull(facadeService);
    }

    @PostMapping("/options")
    public ResponseEntity<WebAuthnChallengeResponse> start(@Valid @RequestBody PasskeyStartRequest request) {
        return ResponseEntity.ok(facadeService.startPasskey(request));
    }

    @PostMapping("/finish")
    public ResponseEntity<WebAuthnAuthenticationResponse> finish(@Valid @RequestBody PasskeyFinishRequest request,
                                                                 HttpServletRequest servletRequest) {
        return ResponseEntity.ok(facadeService.finishPasskey(request, servletRequest));
    }
}
