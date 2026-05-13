package com.tcc.pjb.backend.configs.security.perimeter;

import java.util.List;
import java.util.regex.Pattern;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private static final Pattern IPV4 = Pattern.compile("^(?:\\d{1,3}\\.){3}\\d{1,3}$");

    private final SecurityPerimeterProperties properties;
    private final IpCidrMatcher trustedProxyMatcher;

    public ClientIpResolver(SecurityPerimeterProperties properties) {
        this.properties = properties;
        this.trustedProxyMatcher = new IpCidrMatcher(resolveTrustedCidrs(properties));
    }

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }
        if (properties.isTrustProxyHeaders() && isTrustedProxy(request)) {
            String forwarded = firstForwardedFor(request);
            if (!forwarded.isBlank()) {
                return sanitize(forwarded);
            }
        }
        return sanitize(request.getRemoteAddr());
    }

    public boolean isTrustedProxy(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String remote = sanitize(request.getRemoteAddr());
        if ("UNKNOWN".equals(remote)) {
            return false;
        }
        return trustedProxyMatcher.matches(remote);
    }

    public boolean hasForwardedHeaders(HttpServletRequest request) {
        return headerPresent(request, "Forwarded")
                || headerPresent(request, "X-Forwarded-For")
                || headerPresent(request, "X-Forwarded-Proto")
                || headerPresent(request, "X-Forwarded-Host")
                || headerPresent(request, "X-Forwarded-Port")
                || headerPresent(request, "X-Forwarded-Prefix");
    }

    public int forwardedHopCount(HttpServletRequest request) {
        if (request == null) {
            return 0;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff == null || xff.isBlank()) {
            return 0;
        }
        int count = 0;
        for (String part : xff.split(",")) {
            if (!part.isBlank()) {
                count++;
            }
        }
        return count;
    }

    private boolean headerPresent(HttpServletRequest request, String name) {
        String value = request == null ? null : request.getHeader(name);
        return value != null && !value.isBlank();
    }

    private String firstForwardedFor(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff == null || xff.isBlank()) {
            return "";
        }
        for (String part : xff.split(",")) {
            String candidate = sanitize(part);
            if (!candidate.isBlank() && !"UNKNOWN".equals(candidate)) {
                return candidate;
            }
        }
        return "";
    }

    private List<String> resolveTrustedCidrs(SecurityPerimeterProperties perimeterProperties) {
        if (perimeterProperties.getProxy() == null || perimeterProperties.getProxy().getTrustedCidrs() == null) {
            return List.of();
        }
        return perimeterProperties.getProxy().getTrustedCidrs();
    }

    private String sanitize(String ip) {
        if (ip == null || ip.isBlank()) {
            return "UNKNOWN";
        }
        String value = ip.trim();
        if ("0:0:0:0:0:0:0:1".equals(value) || "::1".equals(value)) {
            return "127.0.0.1";
        }
        if (value.length() > 64) {
            value = value.substring(0, 64);
        }
        value = value.replaceAll("[^0-9a-fA-F:\\.]", "");
        if (value.contains(":")) {
            return value.isBlank() ? "UNKNOWN" : value;
        }
        if (!IPV4.matcher(value).matches()) {
            return "UNKNOWN";
        }
        return value;
    }
}
