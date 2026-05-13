package com.tcc.pjb.backend.core.security.stepup.web;

import java.io.IOException;
import java.time.Instant;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.magistratura.delegation.DelegationCredential;
import com.tcc.pjb.backend.core.security.stepup.FaceReauthTokenPayload;
import com.tcc.pjb.backend.core.security.stepup.FaceReauthTokenService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean({FaceReauthTokenService.class, CurrentUserService.class})
public class MinisterStepUpFilter extends OncePerRequestFilter {

    public static final String HEADER_FACE_TOKEN = "X-PJB-Face-Token";
    private static final long TTL_SECONDS = 3L * 60L * 60L; 

    
    private static final String ISSUE_PATH = "/api/v1/magistratura/face/issue";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith(ISSUE_PATH);
    }

    private final FaceReauthTokenService faceTokenService;
    private final CurrentUserService currentUserService;

    public MinisterStepUpFilter(FaceReauthTokenService faceTokenService, CurrentUserService currentUserService) {
        this.faceTokenService = faceTokenService;
        this.currentUserService = currentUserService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        
        Usuario u = currentUserService.getOrNull();
        if (u == null) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean required = requiresMinisterStepUp(u);
        String token = request.getHeader(HEADER_FACE_TOKEN);
        if (token == null || token.isBlank()) {
            if (required) {
                deny(response, "PJB_FACE_STEPUP_REQUIRED");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        try {
            FaceReauthTokenPayload p = faceTokenService.verifyAndDecode(token);
            if (!u.getId().equals(p.userId())) {
                deny(response, "PJB_FACE_TOKEN_USER_MISMATCH");
                return;
            }

            long now = Instant.now().getEpochSecond();
            long minIat = now - TTL_SECONDS;
            if (p.iat() < minIat || p.exp() < now) {
                deny(response, "PJB_FACE_TOKEN_EXPIRED");
                return;
            }

            RequestContext.setFaceCredential(p);
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException e) {
            deny(response, "PJB_FACE_TOKEN_INVALID");
        }
    }

    private static boolean requiresMinisterStepUp(Usuario u) {
        if (u.getTipoUsuario() == TipoUsuario.MINISTRO) {
            return true;
        }
        DelegationCredential dc = RequestContext.getDelegationCredential().orElse(null);
        if (dc != null && dc.magistrateId() != null) {
            
            
            return dc.issuerTipo() == TipoUsuario.MINISTRO;
        }
        return false;
    }

    private static void deny(HttpServletResponse response, String code) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + code + "\"}");
    }
}
