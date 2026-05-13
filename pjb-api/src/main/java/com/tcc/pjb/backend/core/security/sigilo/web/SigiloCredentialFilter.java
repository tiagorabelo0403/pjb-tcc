package com.tcc.pjb.backend.core.security.sigilo.web;

import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.sigilo.SigiloCredential;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public final class SigiloCredentialFilter extends OncePerRequestFilter {

    public static final String HEADER_REQUEST_ID = "X-PJB-Sigilo-RequestId";
    public static final String HEADER_PASSWORD = "X-PJB-Sigilo-Password";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String rawId = request.getHeader(HEADER_REQUEST_ID);
        String senha = request.getHeader(HEADER_PASSWORD);

        if (rawId != null && !rawId.isBlank() && senha != null && !senha.isBlank()) {
            try {
                UUID id = UUID.fromString(rawId.trim());
                String pwd = sanitizePassword(senha);
                RequestContext.setSigiloCredential(new SigiloCredential(id, pwd));
            } catch (IllegalArgumentException ignore) {
            }
        }

        filterChain.doFilter(request, response);
    }

    private static String sanitizePassword(String raw) {
        String v = raw.trim();
        if (v.length() > 128) v = v.substring(0, 128);
        return v.replaceAll("[\r\n\t]", "");
    }
}
