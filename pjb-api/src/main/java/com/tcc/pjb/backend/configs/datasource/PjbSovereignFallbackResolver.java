
package com.tcc.pjb.backend.configs.datasource;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pjb.datasource.routing", name = "enabled", havingValue = "true")
public class PjbSovereignFallbackResolver {

    private final PjbDataSourceRoutingProperties properties;

    public PjbSovereignFallbackResolver(PjbDataSourceRoutingProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    public SovereignResolution resolve(HttpServletRequest request,
                                       PjbReplicaObservationService.ReplicaObservationSnapshot observation,
                                       boolean critical) {
        PjbDataSourceRoutingProperties.SovereignFallback sovereignFallback = properties.getSovereignFallback();
        if (!sovereignFallback.isEnabled()) {
            return SovereignResolution.disabled();
        }
        String tribunal = header(request, properties.getRegionalSelection().getRequestHeaderTribunal());
        String uf = header(request, properties.getRegionalSelection().getRequestHeaderUf());
        String explicitReplica = normalizeLookupKey(header(request, properties.getRegionalSelection().getRequestHeaderReplica()));
        String requestedReplicaKey = explicitReplica != null ? explicitReplica : desiredReplicaKey(tribunal, uf);
        String scope = scopeFromLookupKey(requestedReplicaKey, tribunal, uf);
        if (requestedReplicaKey != null && isHealthy(requestedReplicaKey, observation, sovereignFallback.getRegionalReplicaLagTolerance())) {
            return new SovereignResolution(scope, requestedReplicaKey, requestedReplicaKey, false, false, "requested-sovereign-replica-healthy");
        }
        String fallbackReplicaKey = fallbackReplicaKey(scope, tribunal, uf);
        if (fallbackReplicaKey != null && isHealthy(fallbackReplicaKey, observation, sovereignFallback.getRegionalReplicaLagTolerance())) {
            return new SovereignResolution(scope, requestedReplicaKey, fallbackReplicaKey, true, false, "fallback-replica-healthy");
        }
        if (critical && sovereignFallback.isForcePrimaryOnCriticalExhaustion()) {
            return new SovereignResolution(scope, requestedReplicaKey, null, true, true, "critical-sovereign-fallback-exhausted");
        }
        if (isHealthy("READ", observation, sovereignFallback.getRegionalReplicaLagTolerance())) {
            return new SovereignResolution(scope, requestedReplicaKey, "READ", requestedReplicaKey != null, false, "shared-read-replica-healthy");
        }
        return new SovereignResolution(scope, requestedReplicaKey, null, requestedReplicaKey != null, false, "no-sovereign-replica-resolution");
    }

    private String desiredReplicaKey(String tribunal, String uf) {
        PjbDataSourceRoutingProperties.RegionalSelection regionalSelection = properties.getRegionalSelection();
        if (tribunal != null && !tribunal.isBlank()) {
            String tribunalKey = compact(tribunal);
            String mapped = regionalSelection.getTribunalToReplica().get(tribunalKey);
            if (mapped != null) {
                return normalizeLookupKey(mapped);
            }
            if (tribunalKey.length() >= 2) {
                String suffix = tribunalKey.substring(tribunalKey.length() - 2);
                if (suffix.chars().allMatch(Character::isLetter)) {
                    String byUf = regionalSelection.getUfToReplica().get(suffix);
                    if (byUf != null) {
                        return normalizeLookupKey(byUf);
                    }
                }
            }
        }
        if (uf != null && !uf.isBlank()) {
            return normalizeLookupKey(regionalSelection.getUfToReplica().get(uf.trim().toUpperCase(Locale.ROOT)));
        }
        return null;
    }

    private String fallbackReplicaKey(String scope, String tribunal, String uf) {
        PjbDataSourceRoutingProperties.SovereignFallback sovereignFallback = properties.getSovereignFallback();
        if (tribunal != null && !tribunal.isBlank()) {
            String tribunalKey = compact(tribunal);
            String mapped = sovereignFallback.getTribunalToFallbackReplica().get(tribunalKey);
            if (mapped != null) {
                return normalizeLookupKey(mapped);
            }
        }
        if (uf != null && !uf.isBlank()) {
            String mapped = sovereignFallback.getUfToFallbackReplica().get(uf.trim().toUpperCase(Locale.ROOT));
            if (mapped != null) {
                return normalizeLookupKey(mapped);
            }
        }
        if (scope != null && !scope.isBlank()) {
            String mapped = sovereignFallback.getScopeToFallbackReplica().get(scope.trim().toUpperCase(Locale.ROOT));
            if (mapped != null) {
                return normalizeLookupKey(mapped);
            }
        }
        return null;
    }

    private boolean isHealthy(String lookupKey,
                              PjbReplicaObservationService.ReplicaObservationSnapshot observation,
                              Duration lagTolerance) {
        if (lookupKey == null || observation == null) {
            return false;
        }
        if ("READ".equals(lookupKey)) {
            return observation.read() != null
                    && observation.read().available()
                    && !lagTooHigh(observation.read().replayLagSeconds(), lagTolerance);
        }
        PjbReplicaObservationService.ReplicaNodeSnapshot snapshot = observation.regional() == null ? null : observation.regional().get(lookupKey);
        return snapshot != null && snapshot.available() && !lagTooHigh(snapshot.replayLagSeconds(), lagTolerance);
    }

    private boolean lagTooHigh(Double replayLagSeconds, Duration lagTolerance) {
        if (replayLagSeconds == null || lagTolerance == null || lagTolerance.isNegative()) {
            return false;
        }
        return replayLagSeconds > lagTolerance.toMillis() / 1000d;
    }

    private String scopeFromLookupKey(String lookupKey, String tribunal, String uf) {
        if (lookupKey != null && lookupKey.startsWith("READ_")) {
            return lookupKey.substring("READ_".length());
        }
        if ("READ".equals(lookupKey)) {
            return "SHARED";
        }
        if (tribunal != null && !tribunal.isBlank()) {
            String compact = compact(tribunal);
            if (compact.startsWith("STJ") || compact.startsWith("STF") || compact.startsWith("TSE")
                    || compact.startsWith("TST") || compact.startsWith("STM") || compact.startsWith("CNJ")) {
                return "SUPERIOR";
            }
        }
        if (uf == null || uf.isBlank()) {
            return null;
        }
        return switch (uf.trim().toUpperCase(Locale.ROOT)) {
            case "AL", "BA", "CE", "MA", "PB", "PE", "PI", "RN", "SE" -> "NORDESTE";
            case "ES", "MG", "RJ", "SP" -> "SUDESTE";
            case "PR", "RS", "SC" -> "SUL";
            case "AC", "AM", "AP", "PA", "RO", "RR", "TO" -> "NORTE";
            case "DF", "GO", "MS", "MT" -> "CENTRO_OESTE";
            default -> null;
        };
    }

    private String header(HttpServletRequest request, String name) {
        if (request == null || name == null || name.isBlank()) {
            return null;
        }
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String compact(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private String normalizeLookupKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String normalized = key.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if ("READ".equals(normalized) || "WRITE".equals(normalized)) {
            return normalized;
        }
        return normalized.startsWith("READ_") ? normalized : "READ_" + normalized;
    }

    public record SovereignResolution(String sovereignScope,
                                      String requestedReplicaKey,
                                      String preferredReplicaKey,
                                      boolean fallbackActivated,
                                      boolean forcePrimary,
                                      String reason) {
        static SovereignResolution disabled() {
            return new SovereignResolution(null, null, null, false, false, "sovereign-fallback-disabled");
        }
    }
}
