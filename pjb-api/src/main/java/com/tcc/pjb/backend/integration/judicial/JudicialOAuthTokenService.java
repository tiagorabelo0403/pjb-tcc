package com.tcc.pjb.backend.integration.judicial;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class JudicialOAuthTokenService {

    private static final int MAX_TRACKED_TOKENS = 256;

    private record CachedToken(String accessToken, Instant expiresAt, Map<String, Object> metadata) {
        boolean valid() {
            return accessToken != null && expiresAt != null && expiresAt.isAfter(Instant.now().plusSeconds(15));
        }
    }

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, CachedToken> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public JudicialOAuthTokenService(RestTemplateBuilder builder, ObjectMapper objectMapper) {
        this.restTemplate = Objects.requireNonNull(builder).build();
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public Optional<String> resolveAccessToken(JudicialSystem system,
                                               JudicialIntegrationProperties.Connector cfg) {
        if (!hasOAuthConfig(cfg)) {
            return Optional.empty();
        }
        cleanupCaches();
        String cacheKey = cacheKey(system, cfg);
        CachedToken cached = cache.get(cacheKey);
        if (cached != null && cached.valid()) {
            return Optional.of(cached.accessToken());
        }
        Object lock = locks.computeIfAbsent(cacheKey, key -> new Object());
        try {
            synchronized (lock) {
                CachedToken current = cache.get(cacheKey);
                if (current != null && current.valid()) {
                    return Optional.of(current.accessToken());
                }
                CachedToken fetched = fetchToken(system, cfg);
                if (fetched == null || !fetched.valid()) {
                    cleanupCaches();
                    return Optional.empty();
                }
                cache.put(cacheKey, fetched);
                cleanupCaches();
                return Optional.of(fetched.accessToken());
            }
        } finally {
            locks.remove(cacheKey, lock);
        }
    }

    public boolean hasOAuthConfig(JudicialIntegrationProperties.Connector cfg) {
        return cfg != null
                && hasText(cfg.getOauthTokenUrl())
                && hasText(cfg.getOauthClientId())
                && hasText(cfg.getOauthClientSecret());
    }

    private CachedToken fetchToken(JudicialSystem system,
                                   JudicialIntegrationProperties.Connector cfg) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(cfg.getOauthTokenUrl().trim()).build(true).toUri();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
            headers.setBasicAuth(cfg.getOauthClientId().trim(), cfg.getOauthClientSecret().trim(), StandardCharsets.UTF_8);
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            putIfHasText(body, "client_id", cfg.getOauthClientId());
            putIfHasText(body, "client_secret", cfg.getOauthClientSecret());
            putIfHasText(body, "audience", cfg.getOauthAudience());
            putIfHasText(body, "scope", cfg.getOauthScope());
            ResponseEntity<String> response = restTemplate.postForEntity(uri, new HttpEntity<>(body, headers), String.class);
            Map<String, Object> payload = parsePayload(response.getBody());
            String accessToken = text(payload.get("access_token"));
            if (!hasText(accessToken)) {
                return null;
            }
            long expiresIn = parseExpiresIn(payload.get("expires_in"));
            Instant expiresAt = Instant.now().plus(Duration.ofSeconds(Math.max(30, expiresIn)));
            LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("system", system != null ? system.name() : null);
            metadata.put("tokenType", text(payload.get("token_type")));
            metadata.put("scope", text(payload.get("scope")));
            metadata.put("expiresIn", expiresIn);
            metadata.entrySet().removeIf(entry -> entry.getValue() == null);
            return new CachedToken(accessToken, expiresAt, Map.copyOf(metadata));
        } catch (RestClientException ex) {
            return null;
        }
    }

    private Map<String, Object> parsePayload(String body) {
        if (body == null || body.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private long parseExpiresIn(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = text(value);
        if (text == null) {
            return 300;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return 300;
        }
    }

    private String cacheKey(JudicialSystem system,
                            JudicialIntegrationProperties.Connector cfg) {
        return (system != null ? system.name() : "OUTRO")
                + '|'
                + text(cfg != null ? cfg.getOauthTokenUrl() : null)
                + '|'
                + text(cfg != null ? cfg.getOauthClientId() : null)
                + '|'
                + text(cfg != null ? cfg.getOauthAudience() : null)
                + '|'
                + text(cfg != null ? cfg.getOauthScope() : null);
    }

    private void putIfHasText(MultiValueMap<String, String> body, String key, String value) {
        if (body != null && key != null && hasText(value)) {
            body.add(key, value.trim());
        }
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String out = String.valueOf(value).trim();
        return out.isBlank() ? null : out;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void cleanupCaches() {
        Instant now = Instant.now();
        cache.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().expiresAt() == null || !entry.getValue().expiresAt().isAfter(now.plusSeconds(15)));
        trimTokenOverflow();
    }

    private void trimTokenOverflow() {
        int overflow = cache.size() - MAX_TRACKED_TOKENS;
        if (overflow <= 0) {
            return;
        }
        cache.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(java.util.Comparator.comparing(CachedToken::expiresAt, java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder()))))
                .limit(overflow)
                .map(Map.Entry::getKey)
                .toList()
                .forEach(cache::remove);
    }


    int cachedTokenCount() {
        return cache.size();
    }

    int lockCount() {
        return locks.size();
    }
}
