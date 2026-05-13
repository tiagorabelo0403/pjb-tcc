package com.tcc.pjb.backend.configs.datasource;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "pjb.datasource.routing", name = "enabled", havingValue = "true")
public class PjbPrimaryReadPreferenceFilter extends OncePerRequestFilter {

    private final PjbPrimaryReadPreferenceContext primaryReadPreferenceContext;
    private final PjbDataSourceRoutingProperties properties;

    public PjbPrimaryReadPreferenceFilter(PjbPrimaryReadPreferenceContext primaryReadPreferenceContext,
                                          PjbDataSourceRoutingProperties properties) {
        this.primaryReadPreferenceContext = Objects.requireNonNull(primaryReadPreferenceContext, "primaryReadPreferenceContext");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            hydratePrimaryPreference(request);
            filterChain.doFilter(request, response);
            propagatePrimaryPreference(request, response);
        } finally {
            primaryReadPreferenceContext.clear();
        }
    }

    private void hydratePrimaryPreference(HttpServletRequest request) {
        PjbDataSourceRoutingProperties.Propagation propagation = properties.getPropagation();
        if (!propagation.isEnabled()) {
            return;
        }
        long headerDeadline = parseDeadline(request.getHeader(propagation.getRequestHeaderName()));
        long cookieDeadline = parseDeadline(extractCookie(request, propagation.getCookieName()));
        long deadline = Math.max(headerDeadline, cookieDeadline);
        if (deadline > System.currentTimeMillis()) {
            primaryReadPreferenceContext.preferPrimaryUntil(deadline);
        }
    }

    private void propagatePrimaryPreference(HttpServletRequest request, HttpServletResponse response) {
        PjbDataSourceRoutingProperties.Propagation propagation = properties.getPropagation();
        if (!propagation.isEnabled()) {
            return;
        }
        long deadline = primaryReadPreferenceContext.currentDeadlineEpochMilli();
        if (deadline > System.currentTimeMillis()) {
            response.setHeader(propagation.getResponseHeaderName(), Long.toString(deadline));
            Duration skew = propagation.getCookieClockSkew() == null ? Duration.ZERO : propagation.getCookieClockSkew();
            long maxAgeSeconds = Math.max(1L, Duration.ofMillis(Math.max(0L, deadline - System.currentTimeMillis()))
                    .plus(skew.isNegative() ? Duration.ZERO : skew)
                    .toSeconds());
            ResponseCookie cookie = ResponseCookie.from(propagation.getCookieName(), Long.toString(deadline))
                    .httpOnly(propagation.isHttpOnlyCookie())
                    .secure(propagation.isSecureCookie() || request.isSecure())
                    .sameSite(defaultIfBlank(propagation.getSameSite(), "Lax"))
                    .path(defaultIfBlank(propagation.getCookiePath(), "/"))
                    .maxAge(maxAgeSeconds)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            return;
        }
        if (hasPropagationCookie(request, propagation)) {
            expireCookie(response, propagation);
        }
    }

    private void expireCookie(HttpServletResponse response, PjbDataSourceRoutingProperties.Propagation propagation) {
        ResponseCookie cookie = ResponseCookie.from(propagation.getCookieName(), "")
                .httpOnly(propagation.isHttpOnlyCookie())
                .secure(propagation.isSecureCookie())
                .sameSite(defaultIfBlank(propagation.getSameSite(), "Lax"))
                .path(defaultIfBlank(propagation.getCookiePath(), "/"))
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private boolean hasPropagationCookie(HttpServletRequest request, PjbDataSourceRoutingProperties.Propagation propagation) {
        return extractCookie(request, propagation.getCookieName()) != null;
    }

    private String extractCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookieName == null || cookieName.isBlank()) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie != null && cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private long parseDeadline(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
