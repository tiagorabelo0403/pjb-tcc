package com.tcc.pjb.backend.configs.security.perimeter;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class ApiRequestOriginSignatureService {

    private static final String HMAC_SHA_256 = "HMAC-SHA256";
    private static final String HEADER_ORIGIN_ID = "X-PJB-Origin-Id";
    private static final String HEADER_TIMESTAMP = "X-PJB-Timestamp";
    private static final String HEADER_SIGNATURE = "X-PJB-Signature";
    private static final String HEADER_SIGNATURE_ALGORITHM = "X-PJB-Signature-Alg";
    private static final String HEADER_BODY_HASH = "X-PJB-Body-Hash";

    private final ApiRequestOriginGovernanceProperties properties;
    private final ClientIpResolver clientIpResolver;

    ApiRequestOriginSignatureService(ApiRequestOriginGovernanceProperties properties,
                                     ClientIpResolver clientIpResolver) {
        this.properties = Objects.requireNonNull(properties);
        this.clientIpResolver = Objects.requireNonNull(clientIpResolver);
    }

    VerificationResult verify(HttpServletRequest request,
                              String requestOrigin,
                              boolean signedRequired) {
        String originId = normalize(request.getHeader(HEADER_ORIGIN_ID));
        if (originId == null) {
            return signedRequired
                    ? VerificationResult.rejected(401, ApiRequestOriginGovernanceMessages.CODE_SIGNED_ORIGIN_ID, ApiRequestOriginGovernanceMessages.DETAIL_SIGNED_ORIGIN_ID)
                    : VerificationResult.missingAttestation();
        }
        ApiRequestOriginGovernanceProperties.TrustedOrigin descriptor = resolve(originId);
        if (descriptor == null) {
            return VerificationResult.rejected(403, ApiRequestOriginGovernanceMessages.CODE_SIGNED_ORIGIN_UNKNOWN, ApiRequestOriginGovernanceMessages.DETAIL_SIGNED_ORIGIN_UNKNOWN);
        }
        if (!isMethodAllowed(descriptor, request.getMethod())) {
            return VerificationResult.rejected(403, ApiRequestOriginGovernanceMessages.CODE_SIGNED_METHOD, ApiRequestOriginGovernanceMessages.DETAIL_SIGNED_METHOD);
        }
        if (!isPathAllowed(descriptor, request.getRequestURI())) {
            return VerificationResult.rejected(403, ApiRequestOriginGovernanceMessages.CODE_SIGNED_PATH, ApiRequestOriginGovernanceMessages.DETAIL_SIGNED_PATH);
        }
        if (!isIpAllowed(descriptor, clientIpResolver.resolve(request))) {
            return VerificationResult.rejected(403, ApiRequestOriginGovernanceMessages.CODE_SIGNED_IP, ApiRequestOriginGovernanceMessages.DETAIL_SIGNED_IP);
        }
        if (!isBrowserOriginAllowed(descriptor, requestOrigin)) {
            return VerificationResult.rejected(403, ApiRequestOriginGovernanceMessages.CODE_SIGNED_BROWSER_ORIGIN, ApiRequestOriginGovernanceMessages.DETAIL_SIGNED_BROWSER_ORIGIN);
        }
        String signatureAlg = normalize(request.getHeader(HEADER_SIGNATURE_ALGORITHM));
        if (signatureAlg != null && !HMAC_SHA_256.equalsIgnoreCase(signatureAlg)) {
            return VerificationResult.rejected(400, ApiRequestOriginGovernanceMessages.CODE_SIGNED_ALGORITHM, ApiRequestOriginGovernanceMessages.DETAIL_SIGNED_ALGORITHM);
        }
        Instant timestamp = parseTimestamp(request.getHeader(HEADER_TIMESTAMP));
        if (timestamp == null || isSkewExceeded(timestamp, properties.getMaxTimestampSkew())) {
            return VerificationResult.rejected(401, ApiRequestOriginGovernanceMessages.CODE_SIGNED_TIMESTAMP, ApiRequestOriginGovernanceMessages.DETAIL_SIGNED_TIMESTAMP);
        }
        String bodyHash = normalizeHex64(request.getHeader(HEADER_BODY_HASH));
        if (requiresBodyHash(request) && bodyHash == null) {
            return VerificationResult.rejected(400, ApiRequestOriginGovernanceMessages.CODE_SIGNED_BODY_HASH, ApiRequestOriginGovernanceMessages.DETAIL_SIGNED_BODY_HASH);
        }
        String computedBodyHash = normalizeHex64(request.getAttribute("PJB_BODY_HASH") instanceof String value ? value : null);
        if (bodyHash != null && computedBodyHash != null && !MessageDigest.isEqual(bodyHash.getBytes(StandardCharsets.UTF_8), computedBodyHash.getBytes(StandardCharsets.UTF_8))) {
            return VerificationResult.rejected(409, ApiRequestOriginGovernanceMessages.CODE_SIGNED_BODY_HASH_MISMATCH, ApiRequestOriginGovernanceMessages.DETAIL_SIGNED_BODY_HASH_MISMATCH);
        }
        String signature = normalize(request.getHeader(HEADER_SIGNATURE));
        if (signature == null) {
            return VerificationResult.rejected(401, ApiRequestOriginGovernanceMessages.CODE_SIGNED_SIGNATURE, ApiRequestOriginGovernanceMessages.DETAIL_SIGNED_SIGNATURE);
        }
        String material = canonicalMaterial(originId, timestamp, request.getMethod(), request.getRequestURI(), bodyHash);
        if (!matches(descriptor.getSecret(), material, signature)) {
            return VerificationResult.rejected(401, ApiRequestOriginGovernanceMessages.CODE_SIGNED_SIGNATURE, ApiRequestOriginGovernanceMessages.DETAIL_SIGNED_SIGNATURE);
        }
        return VerificationResult.allowed(originId, descriptor, bodyHash);
    }

    String extractRequestOrigin(HttpServletRequest request, boolean allowRefererFallback) {
        String origin = normalize(request.getHeader("Origin"));
        if (origin != null && !"null".equalsIgnoreCase(origin)) {
            return sanitizeOrigin(origin);
        }
        if (!allowRefererFallback) {
            return null;
        }
        String referer = normalize(request.getHeader("Referer"));
        if (referer == null) {
            return null;
        }
        try {
            URI uri = URI.create(referer);
            String scheme = normalize(uri.getScheme());
            String host = normalize(uri.getHost());
            if (scheme == null || host == null) {
                return null;
            }
            int port = uri.getPort();
            return port > 0
                    ? scheme.toLowerCase(Locale.ROOT) + "://" + host.toLowerCase(Locale.ROOT) + ':' + port
                    : scheme.toLowerCase(Locale.ROOT) + "://" + host.toLowerCase(Locale.ROOT);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private ApiRequestOriginGovernanceProperties.TrustedOrigin resolve(String originId) {
        for (ApiRequestOriginGovernanceProperties.TrustedOrigin origin : properties.getTrustedOrigins()) {
            if (origin != null && origin.isActive() && originId.equals(origin.getId())) {
                return origin;
            }
        }
        return null;
    }

    private boolean isMethodAllowed(ApiRequestOriginGovernanceProperties.TrustedOrigin descriptor, String method) {
        if (descriptor.getAllowedMethods().isEmpty()) {
            return true;
        }
        String normalized = normalize(method);
        if (normalized == null) {
            return false;
        }
        return descriptor.getAllowedMethods().contains(normalized.toUpperCase(Locale.ROOT));
    }

    private boolean isPathAllowed(ApiRequestOriginGovernanceProperties.TrustedOrigin descriptor, String path) {
        if (descriptor.getAllowedPathPrefixes().isEmpty()) {
            return true;
        }
        for (String prefix : descriptor.getAllowedPathPrefixes()) {
            if (prefix != null && !prefix.isBlank() && path != null && path.startsWith(prefix.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean isIpAllowed(ApiRequestOriginGovernanceProperties.TrustedOrigin descriptor, String ip) {
        if (descriptor.getAllowedCidrs().isEmpty()) {
            return true;
        }
        return new IpCidrMatcher(descriptor.getAllowedCidrs()).matches(ip);
    }

    private boolean isBrowserOriginAllowed(ApiRequestOriginGovernanceProperties.TrustedOrigin descriptor, String requestOrigin) {
        if (requestOrigin == null || descriptor.getAllowedOrigins().isEmpty()) {
            return true;
        }
        for (String allowed : descriptor.getAllowedOrigins()) {
            if (requestOrigin.equalsIgnoreCase(allowed)) {
                return true;
            }
        }
        return false;
    }

    private boolean requiresBodyHash(HttpServletRequest request) {
        if (!properties.isRequireBodyHashOnSignedJsonRequests()) {
            return false;
        }
        long contentLength = request.getContentLengthLong();
        if (contentLength <= 0L) {
            return false;
        }
        String contentType = normalize(request.getContentType());
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("application/json");
    }

    private boolean isSkewExceeded(Instant timestamp, Duration maxSkew) {
        Duration skew = Duration.between(timestamp, Instant.now()).abs();
        return skew.compareTo(maxSkew) > 0;
    }

    private Instant parseTimestamp(String raw) {
        String value = normalize(raw);
        if (value == null) {
            return null;
        }
        try {
            if (value.chars().allMatch(Character::isDigit)) {
                long numeric = Long.parseLong(value);
                return value.length() >= 13 ? Instant.ofEpochMilli(numeric) : Instant.ofEpochSecond(numeric);
            }
            return Instant.parse(value);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private boolean matches(String secret, String material, String signature) {
        String expected = sign(secret, material);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.trim().getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String secret, String material) {
        String effectiveSecret = normalize(secret);
        if (effectiveSecret == null) {
            throw new IllegalStateException("Segredo de origem confiavel nao configurado.");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(effectiveSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(material.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao assinar material de origem soberana.", ex);
        }
    }

    private String canonicalMaterial(String originId,
                                     Instant timestamp,
                                     String method,
                                     String path,
                                     String bodyHash) {
        return normalize(originId) + '\n'
                + timestamp.toString() + '\n'
                + normalize(method).toUpperCase(Locale.ROOT) + '\n'
                + normalize(path) + '\n'
                + (bodyHash == null ? "" : bodyHash);
    }

    private String sanitizeOrigin(String origin) {
        try {
            URI uri = URI.create(origin);
            String scheme = normalize(uri.getScheme());
            String host = normalize(uri.getHost());
            if (scheme == null || host == null) {
                return null;
            }
            int port = uri.getPort();
            return port > 0
                    ? scheme.toLowerCase(Locale.ROOT) + "://" + host.toLowerCase(Locale.ROOT) + ':' + port
                    : scheme.toLowerCase(Locale.ROOT) + "://" + host.toLowerCase(Locale.ROOT);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeHex64(String value) {
        String normalized = normalize(value);
        if (normalized == null || normalized.length() != 64) {
            return null;
        }
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) {
                return null;
            }
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    record VerificationResult(boolean allowed,
                              boolean attestationMissing,
                              int status,
                              String code,
                              String detail,
                              String originId,
                              ApiRequestOriginGovernanceProperties.TrustedOrigin trustedOrigin,
                              String bodyHash) {

        static VerificationResult allowed(String originId,
                                          ApiRequestOriginGovernanceProperties.TrustedOrigin trustedOrigin,
                                          String bodyHash) {
            return new VerificationResult(true, false, 0, null, null, originId, trustedOrigin, bodyHash);
        }

        static VerificationResult rejected(int status, String code, String detail) {
            return new VerificationResult(false, false, status, code, detail, null, null, null);
        }

        static VerificationResult missingAttestation() {
            return new VerificationResult(false, true, 0, null, null, null, null, null);
        }
    }
}
