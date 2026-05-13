package com.tcc.pjb.backend.controller.auth;

import com.tcc.pjb.backend.model.dto.security.StepUpFinishRequest;
import com.tcc.pjb.backend.model.dto.security.StepUpStartRequest;
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
@RequestMapping("/api/v1/auth/stepup")
@PreAuthorize("permitAll()")
public class PasskeyStepUpController {

    private final WebAuthnSurfaceFacadeService facadeService;

    public PasskeyStepUpController(WebAuthnSurfaceFacadeService facadeService) {
        this.facadeService = Objects.requireNonNull(facadeService);
    }

    @PostMapping("/options")
    public ResponseEntity<WebAuthnChallengeResponse> start(@Valid @RequestBody StepUpStartRequest request) {
        return ResponseEntity.ok(facadeService.startStepUp(request));
    }

    @PostMapping("/finish")
    public ResponseEntity<WebAuthnAuthenticationResponse> finish(@Valid @RequestBody StepUpFinishRequest request,
                                                                 HttpServletRequest servletRequest) {
        return ResponseEntity.ok(facadeService.finishStepUp(request, servletRequest));
    }
}
