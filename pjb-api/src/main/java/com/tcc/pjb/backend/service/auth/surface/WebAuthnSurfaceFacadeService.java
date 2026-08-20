package com.tcc.pjb.backend.service.auth.surface;

import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.webauthn.WebAuthnService;
import com.tcc.pjb.backend.model.dto.security.PasskeyFinishRequest;
import com.tcc.pjb.backend.model.dto.security.PasskeyStartRequest;
import com.tcc.pjb.backend.model.dto.security.StepUpFinishRequest;
import com.tcc.pjb.backend.model.dto.security.StepUpStartRequest;
import com.tcc.pjb.backend.model.dto.security.WebAuthnAuthenticationResponse;
import com.tcc.pjb.backend.model.dto.security.WebAuthnChallengeResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class WebAuthnSurfaceFacadeService {

    private final CurrentUserService currentUserService;
    private final WebAuthnService webAuthnService;
    private final ClientIpResolver ipResolver;

    public WebAuthnSurfaceFacadeService(CurrentUserService currentUserService,
                                        WebAuthnService webAuthnService,
                                        ClientIpResolver ipResolver) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.webAuthnService = Objects.requireNonNull(webAuthnService);
        this.ipResolver = Objects.requireNonNull(ipResolver);
    }

    public WebAuthnChallengeResponse startPasskey(PasskeyStartRequest request) {
        var start = webAuthnService.startPasskeyLogin(request.getEmail());
        return new WebAuthnChallengeResponse(start.sessionId(), start.optionsJson());
    }

    public WebAuthnAuthenticationResponse finishPasskey(PasskeyFinishRequest request, HttpServletRequest servletRequest) {
        String ip = ipResolver.resolve(servletRequest);
        var result = webAuthnService.finishPasskeyLogin(request.getSessionId(), request.getCredentialJson(), ip);
        return new WebAuthnAuthenticationResponse(result.token(), result.expiresAt(), result.deviceId(), result.termosPendentes());
    }

    public WebAuthnChallengeResponse startStepUp(StepUpStartRequest request) {
        Usuario usuario = currentUserService.getRequired();
        var start = webAuthnService.startStepUp(usuario, request.getAction(), request.getRequestHash());
        return new WebAuthnChallengeResponse(start.sessionId(), start.optionsJson());
    }

    public WebAuthnAuthenticationResponse finishStepUp(StepUpFinishRequest request, HttpServletRequest servletRequest) {
        Usuario usuario = currentUserService.getRequired();
        String ip = ipResolver.resolve(servletRequest);
        var result = webAuthnService.finishStepUp(usuario, request.getSessionId(), request.getCredentialJson(), ip);
        return new WebAuthnAuthenticationResponse(result.token(), result.expiresAt(), result.deviceId(), result.termosPendentes());
    }
}
