package com.tcc.pjb.backend.configs.api;

import java.net.URI;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

public final class GovBrHttpResponses {

    private GovBrHttpResponses() {
    }

    public static ResponseEntity<Void> redirectOrNoContent(String redirect) {
        if (redirect == null || redirect.isBlank()) {
            return noContent();
        }
        HttpHeaders headers = hardenedHeaders();
        headers.setLocation(sanitizeRedirect(redirect));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    public static ResponseEntity<Void> noContent() {
        return new ResponseEntity<>(hardenedHeaders(), HttpStatus.NO_CONTENT);
    }

    private static HttpHeaders hardenedHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl("no-store, no-cache, max-age=0, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);
        headers.set("X-Robots-Tag", "noindex, nofollow, noarchive");
        headers.set("Referrer-Policy", "no-referrer");
        headers.set("X-Content-Type-Options", "nosniff");
        return headers;
    }

    static URI sanitizeRedirect(String redirect) {
        String trimmed = redirect == null ? "" : redirect.trim();
        if (trimmed.isEmpty() || trimmed.contains("\r") || trimmed.contains("\n") || trimmed.contains("\\")) {
            throw invalidRedirect();
        }
        URI uri;
        try {
            uri = URI.create(trimmed).normalize();
        } catch (Exception e) {
            throw invalidRedirect();
        }
        if (uri.getFragment() != null || uri.getUserInfo() != null) {
            throw invalidRedirect();
        }
        if (!uri.isAbsolute()) {
            if (!trimmed.startsWith("/") || trimmed.startsWith("//")) {
                throw invalidRedirect();
            }
            return uri;
        }
        String scheme = lower(uri.getScheme());
        String host = lower(uri.getHost());
        boolean loopback = "localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host);
        boolean validScheme = "https".equals(scheme) || (loopback && "http".equals(scheme));
        if (!validScheme || host.isBlank()) {
            throw invalidRedirect();
        }
        return uri;
    }

    private static ResponseStatusException invalidRedirect() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_redirect");
    }

    private static String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
