package com.tcc.pjb.backend.service.api.oauth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.util.DeterministicUuid;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.api.MarketplaceAccessTokenRecord;
import com.tcc.pjb.backend.model.entity.api.MarketplaceAuditEvent;
import com.tcc.pjb.backend.model.entity.api.MarketplaceClientApp;
import com.tcc.pjb.backend.model.repository.MarketplaceAccessTokenRecordRepository;
import com.tcc.pjb.backend.model.repository.MarketplaceAuditEventRepository;
import com.tcc.pjb.backend.model.repository.MarketplaceClientAppRepository;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Service
public class MarketplaceOAuth2Service {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ISSUER = "PJB-MARKETPLACE";
    private static final String AUDIENCE = "PJB-INTEGRADORES";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final MarketplaceClientAppRepository clientRepository;
    private final MarketplaceAccessTokenRecordRepository tokenRepository;
    private final MarketplaceAuditEventRepository auditRepository;
    private final ObjectMapper objectMapper;

    public MarketplaceOAuth2Service(MarketplaceClientAppRepository clientRepository,
                                    MarketplaceAccessTokenRecordRepository tokenRepository,
                                    MarketplaceAuditEventRepository auditRepository,
                                    ObjectMapper objectMapper) {
        this.clientRepository = Objects.requireNonNull(clientRepository);
        this.tokenRepository = Objects.requireNonNull(tokenRepository);
        this.auditRepository = Objects.requireNonNull(auditRepository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @PjbTransactionalBudget(operation = "marketplace.oauth2.listar-clientes", maxMillis = 3000)
    @Transactional(readOnly = true)
    public List<ClientView> listarClientes() {
        return clientRepository.findAll().stream()
                .sorted((a, b) -> compareInstantsDesc(a.getCreatedAt(), b.getCreatedAt()))
                .map(this::toView)
                .toList();
    }

    @Transactional
    public ClientRegistrationResponse registrarCliente(ClientRegistrationRequest request, String ipAddress) {
        String rawClientId = normalizeToken(request.clientId());
        String clientId = rawClientId.isBlank() ? autoClientId(request.displayName()) : rawClientId;
        if (clientRepository.existsByClientIdIgnoreCase(clientId)) {
            throw new IllegalArgumentException("Client id já cadastrado.");
        }

        String plainSecret = randomSecret();
        MarketplaceClientApp client = new MarketplaceClientApp();
        client.setClientId(clientId);
        client.setClientSecretHash(Hashes.sha256Hex(plainSecret));
        client.setDisplayName(defaultText(request.displayName(), clientId));
        client.setOwnerName(defaultText(request.ownerName(), request.displayName()));
        client.setOwnerEmail(request.ownerEmail());
        client.setAllowedScopes(joinScopes(request.allowedScopes()));
        client.setAllowedGrants("client_credentials");
        client.setStatus("ATIVO");
        client.setTrustedOrigin(request.trustedOrigin());
        client.setAccessTokenTtlSeconds(request.accessTokenTtlSeconds() == null || request.accessTokenTtlSeconds() <= 0 ? 900 : request.accessTokenTtlSeconds());
        MarketplaceClientApp saved = clientRepository.save(client);

        audit(saved, "CLIENT_REGISTERED", null, clientId, "Cliente OAuth2 do marketplace registrado.", ipAddress);
        return new ClientRegistrationResponse(saved.getId(), saved.getClientId(), plainSecret, saved.getDisplayName(),
                splitScopes(saved.getAllowedScopes()).stream().toList(), saved.getAccessTokenTtlSeconds(), saved.getCreatedAt());
    }

    @Transactional
    public TokenResponse emitirToken(TokenRequest request, String ipAddress) {
        if (!"client_credentials".equalsIgnoreCase(defaultText(request.grantType(), "client_credentials"))) {
            throw new IllegalArgumentException("Grant type não suportado.");
        }

        MarketplaceClientApp client = clientRepository.findByClientIdIgnoreCase(defaultText(request.clientId(), ""))
                .orElseThrow(() -> new IllegalArgumentException("Client id inválido."));
        ensureClientActive(client);
        if (!supportsGrant(client, "client_credentials")) {
            throw new MarketplaceOAuthException(org.springframework.http.HttpStatus.FORBIDDEN, "Cliente não habilitado para client_credentials.");
        }
        if (!secretMatches(client, request.clientSecret())) {
            audit(client, "TOKEN_DENIED", null, client.getClientId(), "Falha de autenticação do client secret.", ipAddress);
            throw new IllegalArgumentException("Credenciais inválidas.");
        }

        Set<String> allowedScopes = splitScopes(client.getAllowedScopes());
        Set<String> effectiveScopes = resolveEffectiveScopes(allowedScopes, request.scope());
        if (effectiveScopes.isEmpty()) {
            throw new IllegalArgumentException("Escopo inválido para o cliente.");
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(client.getAccessTokenTtlSeconds() == null ? 900L : client.getAccessTokenTtlSeconds().longValue());
        String jti = DeterministicUuid.v5("marketplace-oauth-token", client.getClientId() + "|" + now.toEpochMilli() + "|" + randomSuffix()).toString();

        LinkedHashMap<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "at+jwt");

        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", ISSUER);
        payload.put("aud", AUDIENCE);
        payload.put("sub", client.getClientId());
        payload.put("client_id", client.getClientId());
        payload.put("scope", String.join(" ", effectiveScopes));
        payload.put("jti", jti);
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String headerEncoded = encodeJson(header);
        String payloadEncoded = encodeJson(payload);
        String signingInput = headerEncoded + "." + payloadEncoded;
        String signature = sign(signingInput, client);
        String accessToken = signingInput + "." + signature;

        MarketplaceAccessTokenRecord token = new MarketplaceAccessTokenRecord();
        token.setJti(jti);
        token.setClientApp(client);
        token.setScope(String.join(" ", effectiveScopes));
        token.setTokenHash(Hashes.sha256Hex(accessToken));
        token.setStatus("ATIVO");
        token.setIssuedAt(now);
        token.setExpiresAt(expiresAt);
        tokenRepository.save(token);

        client.setLastAuthenticatedAt(now);
        clientRepository.save(client);

        audit(client, "TOKEN_ISSUED", jti, client.getClientId(), "Access token emitido para o marketplace.", ipAddress);
        return new TokenResponse(accessToken, "Bearer", Math.max(1L, expiresAt.getEpochSecond() - now.getEpochSecond()), String.join(" ", effectiveScopes), client.getClientId(), jti);
    }

    @Transactional
    public IntrospectionResponse introspect(String tokenValue, String ipAddress) {
        TokenResolution resolution = resolveActiveToken(tokenValue, null, ipAddress, false);
        MarketplaceAccessTokenRecord token = resolution.record();
        token.setLastIntrospectionAt(Instant.now());
        tokenRepository.save(token);
        audit(resolution.client(), "TOKEN_INTROSPECTED", token.getJti(), resolution.client().getClientId(), "Token introspectado.", ipAddress);
        return new IntrospectionResponse(
                true,
                resolution.client().getClientId(),
                resolution.record().getJti(),
                resolution.record().getIssuedAt(),
                resolution.record().getExpiresAt(),
                splitScopes(resolution.record().getScope()),
                resolution.claims()
        );
    }

    @Transactional
    public Map<String, Object> revogar(String tokenValue, String motivo, String ipAddress) {
        TokenResolution resolution = resolveActiveToken(tokenValue, null, ipAddress, false);
        MarketplaceAccessTokenRecord token = resolution.record();
        token.setStatus("REVOGADO");
        token.setRevokedAt(Instant.now());
        tokenRepository.save(token);
        audit(resolution.client(), "TOKEN_REVOKED", token.getJti(), resolution.client().getClientId(),
                defaultText(motivo, "Access token revogado."), ipAddress);
        return Map.of(
                "status", "REVOGADO",
                "clientId", resolution.client().getClientId(),
                "jti", token.getJti(),
                "revogadoEm", token.getRevokedAt()
        );
    }

    @Transactional
    public AuthorizedClient authorizeHttpRequest(HttpServletRequest request, String... requiredScopes) {
        String header = request != null ? request.getHeader("Authorization") : null;
        TokenResolution resolution = resolveActiveToken(extractBearerToken(header), requiredScopes == null ? Set.of() : Set.of(requiredScopes), resolveIp(request), true);
        resolution.record().setLastUsedAt(Instant.now());
        tokenRepository.save(resolution.record());
        return new AuthorizedClient(resolution.client().getClientId(), splitScopes(resolution.record().getScope()), resolution.record().getJti());
    }

    @Transactional
    public AuthorizedClient authorizeBearerToken(String tokenValue, Collection<String> requiredScopes, String ipAddress) {
        TokenResolution resolution = resolveActiveToken(tokenValue, requiredScopes, ipAddress, true);
        resolution.record().setLastUsedAt(Instant.now());
        tokenRepository.save(resolution.record());
        return new AuthorizedClient(resolution.client().getClientId(), splitScopes(resolution.record().getScope()), resolution.record().getJti());
    }

    private TokenResolution resolveActiveToken(String tokenValue,
                                               Collection<String> requiredScopes,
                                               String ipAddress,
                                               boolean emitUsageAudit) {
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new IllegalArgumentException("Bearer token ausente.");
        }
        ParsedToken parsed = parseAndValidate(tokenValue);
        MarketplaceClientApp client = clientRepository.findByClientIdIgnoreCase(parsed.clientId())
                .orElseThrow(() -> new IllegalArgumentException("Client do token não localizado."));
        ensureClientActive(client);
        verifySignature(parsed.signingInput(), parsed.signature(), client);

        MarketplaceAccessTokenRecord record = tokenRepository.findByJti(parsed.jti())
                .orElseThrow(() -> new IllegalArgumentException("Token não encontrado na trilha de auditoria."));
        if (!MessageDigest.isEqual(record.getTokenHash().getBytes(StandardCharsets.UTF_8), Hashes.sha256Hex(tokenValue).getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Token inválido.");
        }
        if (!"ATIVO".equalsIgnoreCase(defaultText(record.getStatus(), ""))) {
            throw new MarketplaceOAuthException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Token não está ativo.");
        }
        if (record.getExpiresAt() == null || record.getExpiresAt().isBefore(Instant.now())) {
            record.setStatus("EXPIRADO");
            tokenRepository.save(record);
            throw new MarketplaceOAuthException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Token expirado.");
        }
        Set<String> required = normalizeScopes(requiredScopes);
        Set<String> current = splitScopes(record.getScope());
        if (!current.containsAll(required)) {
            throw new MarketplaceOAuthException(org.springframework.http.HttpStatus.FORBIDDEN, "Escopo insuficiente.");
        }
        if (emitUsageAudit) {
            audit(client, "TOKEN_USED", record.getJti(), client.getClientId(),
                    "Token utilizado em endpoint protegido do marketplace.", ipAddress);
        }
        return new TokenResolution(client, record, parsed.claims());
    }

    private ParsedToken parseAndValidate(String tokenValue) {
        String[] parts = tokenValue.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Formato de token inválido.");
        }
        Map<String, Object> header = decodeJson(parts[0]);
        Map<String, Object> claims = decodeJson(parts[1]);
        if (!"HS256".equals(String.valueOf(header.get("alg")))) {
            throw new IllegalArgumentException("Algoritmo do token não suportado.");
        }
        String issuer = String.valueOf(claims.getOrDefault("iss", ""));
        if (!ISSUER.equals(issuer)) {
            throw new IllegalArgumentException("Issuer inválido.");
        }
        long exp = parseEpoch(claims.get("exp"));
        if (exp <= Instant.now().getEpochSecond()) {
            throw new MarketplaceOAuthException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Token expirado.");
        }
        String clientId = String.valueOf(claims.getOrDefault("client_id", ""));
        String jti = String.valueOf(claims.getOrDefault("jti", ""));
        if (clientId.isBlank() || jti.isBlank()) {
            throw new IllegalArgumentException("Claims obrigatórias ausentes.");
        }
        return new ParsedToken(clientId, jti, parts[0] + "." + parts[1], parts[2], claims);
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar token.", e);
        }
    }

    private Map<String, Object> decodeJson(String encoded) {
        try {
            return objectMapper.readValue(URL_DECODER.decode(encoded), MAP_TYPE);
        } catch (Exception e) {
            throw new IllegalArgumentException("Token malformado.", e);
        }
    }

    private String sign(String signingInput, MarketplaceClientApp client) {
        byte[] key = signingKey(client);
        byte[] data = signingInput.getBytes(StandardCharsets.UTF_8);
        return URL_ENCODER.encodeToString(hmacSha256(key, data));
    }

    private void verifySignature(String signingInput, String signature, MarketplaceClientApp client) {
        String expected = sign(signingInput, client);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Assinatura do token inválida.");
        }
    }

    private byte[] signingKey(MarketplaceClientApp client) {
        String seed = "PJB-MARKETPLACE-OAUTH2-2026|" + defaultText(client.getClientId(), "") + "|" + defaultText(client.getClientSecretHash(), "");
        return Hashes.sha256(seed.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao assinar token.", e);
        }
    }

    private boolean secretMatches(MarketplaceClientApp client, String plainSecret) {
        String expectedHash = defaultText(client.getClientSecretHash(), "");
        String providedHash = Hashes.sha256Hex(defaultText(plainSecret, ""));
        return MessageDigest.isEqual(expectedHash.getBytes(StandardCharsets.UTF_8), providedHash.getBytes(StandardCharsets.UTF_8));
    }

    private void ensureClientActive(MarketplaceClientApp client) {
        if (!"ATIVO".equalsIgnoreCase(defaultText(client.getStatus(), ""))) {
            throw new MarketplaceOAuthException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Cliente OAuth2 inativo.");
        }
    }

    private boolean supportsGrant(MarketplaceClientApp client, String grant) {
        return splitScopes(client.getAllowedGrants()).contains(normalizeScope(grant));
    }

    private Set<String> resolveEffectiveScopes(Set<String> allowed, String requestedScopes) {
        Set<String> requested = splitScopes(requestedScopes);
        if (requested.isEmpty()) {
            return allowed;
        }
        LinkedHashSet<String> effective = new LinkedHashSet<>();
        for (String scope : requested) {
            if (allowed.contains(scope)) {
                effective.add(scope);
            }
        }
        return effective;
    }

    private void audit(MarketplaceClientApp client, String eventType, String jti, String subject, String summary, String ipAddress) {
        MarketplaceAuditEvent event = new MarketplaceAuditEvent();
        event.setClientApp(client);
        event.setEventType(eventType);
        event.setJti(jti);
        event.setSubject(subject);
        event.setEventSummary(summary);
        event.setIpAddress(ipAddress);
        auditRepository.save(event);
    }

    private ClientView toView(MarketplaceClientApp client) {
        return new ClientView(
                client.getId(),
                client.getClientId(),
                client.getDisplayName(),
                client.getOwnerName(),
                client.getOwnerEmail(),
                splitScopes(client.getAllowedScopes()).stream().toList(),
                client.getStatus(),
                client.getTrustedOrigin(),
                client.getAccessTokenTtlSeconds(),
                client.getLastAuthenticatedAt(),
                client.getCreatedAt()
        );
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        String header = authorizationHeader.trim();
        if (header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return header.substring(7).trim();
        }
        return header;
    }

    private String randomSecret() {
        byte[] buffer = new byte[24];
        RANDOM.nextBytes(buffer);
        return URL_ENCODER.encodeToString(buffer);
    }

    private String autoClientId(String displayName) {
        String base = normalizeToken(defaultText(displayName, "integrador")).replace('_', '-').toLowerCase(Locale.ROOT);
        if (base.isBlank()) {
            base = "integrador";
        }
        return "pjb-" + base + "-" + randomSuffix().substring(0, 8);
    }

    private String randomSuffix() {
        byte[] buffer = new byte[8];
        RANDOM.nextBytes(buffer);
        return URL_ENCODER.encodeToString(buffer).toLowerCase(Locale.ROOT);
    }

    private Set<String> splitScopes(String scopes) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (scopes == null || scopes.isBlank()) {
            return out;
        }
        for (String raw : scopes.split("[,\\s]+")) {
            String normalized = normalizeScope(raw);
            if (!normalized.isBlank()) {
                out.add(normalized);
            }
        }
        return out;
    }

    private Set<String> normalizeScopes(Collection<String> scopes) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (scopes == null) {
            return out;
        }
        for (String scope : scopes) {
            String normalized = normalizeScope(scope);
            if (!normalized.isBlank()) {
                out.add(normalized);
            }
        }
        return out;
    }

    private String joinScopes(Collection<String> scopes) {
        Set<String> normalized = normalizeScopes(scopes);
        return normalized.isEmpty() ? "processos:protocolar processos:documentos" : String.join(" ", normalized);
    }

    private String normalizeScope(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String token = raw.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9:_-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        return token;
    }

    private long parseEpoch(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private int compareInstantsDesc(Instant a, Instant b) {
        Instant ia = a == null ? Instant.EPOCH : a;
        Instant ib = b == null ? Instant.EPOCH : b;
        return ib.compareTo(ia);
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private record ParsedToken(String clientId, String jti, String signingInput, String signature, Map<String, Object> claims) {
    }

    private record TokenResolution(MarketplaceClientApp client, MarketplaceAccessTokenRecord record, Map<String, Object> claims) {
    }

    public record ClientRegistrationRequest(
            String clientId,
            String displayName,
            String ownerName,
            String ownerEmail,
            List<String> allowedScopes,
            String trustedOrigin,
            Integer accessTokenTtlSeconds
    ) {
    }

    public record ClientRegistrationResponse(
            Long id,
            String clientId,
            String clientSecret,
            String displayName,
            List<String> allowedScopes,
            Integer accessTokenTtlSeconds,
            Instant createdAt
    ) {
    }

    public record ClientView(
            Long id,
            String clientId,
            String displayName,
            String ownerName,
            String ownerEmail,
            List<String> allowedScopes,
            String status,
            String trustedOrigin,
            Integer accessTokenTtlSeconds,
            Instant lastAuthenticatedAt,
            Instant createdAt
    ) {
    }

    public record TokenRequest(
            String clientId,
            String clientSecret,
            String grantType,
            String scope
    ) {
    }

    public record TokenResponse(
            String accessToken,
            String tokenType,
            Long expiresIn,
            String scope,
            String clientId,
            String jti
    ) {
    }

    public record IntrospectionResponse(
            boolean active,
            String clientId,
            String jti,
            Instant issuedAt,
            Instant expiresAt,
            Set<String> scopes,
            Map<String, Object> claims
    ) {
    }

    public record AuthorizedClient(
            String clientId,
            Set<String> scopes,
            String jti
    ) {
    }
}
