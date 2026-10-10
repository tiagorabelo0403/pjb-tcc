package com.tcc.pjb.backend.configs.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

public final class BearerDoEmissorExternoResolver implements BearerTokenResolver {

    private static final Pattern FORMATO_JWT = Pattern.compile("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]*$");

    private static final String ROTAS_DO_MARKETPLACE = "/api/marketplace/";

    private final BearerTokenResolver padrao = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        if (request.getRequestURI().startsWith(request.getContextPath() + ROTAS_DO_MARKETPLACE)) {
            return null;
        }
        String token = padrao.resolve(request);
        return token != null && FORMATO_JWT.matcher(token).matches() ? token : null;
    }
}
