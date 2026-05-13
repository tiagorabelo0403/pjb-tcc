package com.tcc.pjb.backend.configs.security.perimeter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ForwardedHeaderGuardFilter extends OncePerRequestFilter {

    private final SecurityPerimeterProperties properties;
    private final ClientIpResolver clientIpResolver;
    private final ObjectMapper objectMapper;

    public ForwardedHeaderGuardFilter(SecurityPerimeterProperties properties,
                                      ClientIpResolver clientIpResolver,
                                      ObjectMapper objectMapper) {
        this.properties = properties;
        this.clientIpResolver = clientIpResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request == null || !properties.isEnabled() || !clientIpResolver.hasForwardedHeaders(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        SecurityPerimeterProperties.Proxy proxy = properties.getProxy();
        if (!properties.isTrustProxyHeaders()) {
            writeJson(response, 400, "FORWARDED_HEADERS_DISABLED", "Cabeçalhos de proxy não são aceitos neste perímetro.");
            return;
        }
        if (proxy != null && proxy.isRejectUntrustedForwardedHeaders() && !clientIpResolver.isTrustedProxy(request)) {
            writeJson(response, 400, "UNTRUSTED_PROXY", "Cabeçalhos de proxy recebidos de origem não confiável.");
            return;
        }
        int hopCount = clientIpResolver.forwardedHopCount(request);
        int maxHops = proxy == null ? 0 : proxy.getMaxForwardedForHops();
        if (maxHops > 0 && hopCount > maxHops) {
            writeJson(response, 400, "FORWARDED_CHAIN_TOO_LONG", "Cadeia de encaminhamento acima do limite permitido.");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeJson(HttpServletResponse response,
                           int status,
                           String code,
                           String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.getWriter().flush();
    }
}
