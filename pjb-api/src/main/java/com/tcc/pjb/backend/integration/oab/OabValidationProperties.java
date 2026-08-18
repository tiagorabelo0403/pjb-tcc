package com.tcc.pjb.backend.integration.oab;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.integrations.oab")
public record OabValidationProperties(
        boolean enabled,
        String baseUrl,
        String validationPath,
        String apiKey,
        String apiKeyHeader,
        Duration requestTimeout,
        boolean allowIndeterminate,
        boolean allowIndeterminateInNonProduction,
        boolean warnOnIndeterminateAllowed
) {
    public OabValidationProperties {
        baseUrl = trimToNull(baseUrl);
        validationPath = trimToNull(validationPath);
        if (validationPath == null) {
            validationPath = "/advogados/{uf}/{numero}";
        }
        apiKey = trimToNull(apiKey);
        apiKeyHeader = trimToNull(apiKeyHeader);
        if (apiKeyHeader == null) {
            apiKeyHeader = "X-API-Key";
        }
        if (requestTimeout == null || requestTimeout.isZero() || requestTimeout.isNegative()) {
            requestTimeout = Duration.ofSeconds(3);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
