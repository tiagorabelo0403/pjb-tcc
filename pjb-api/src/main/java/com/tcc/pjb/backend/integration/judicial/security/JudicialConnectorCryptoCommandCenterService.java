package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorCryptographicFailureEvent;
import com.tcc.pjb.backend.model.repository.JudicialConnectorCryptographicFailureEventRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JudicialConnectorCryptoCommandCenterService {

    private final JudicialConnectorCertificateInventoryService inventoryService;
    private final JudicialConnectorSecurityPackService packService;
    private final JudicialConnectorCryptoAdminOpsService adminOpsService;
    private final JudicialConnectorSecuritySessionService sessionService;
    private final JudicialConnectorCryptographicFailureEventRepository failureRepository;

    public JudicialConnectorCryptoCommandCenterService(JudicialConnectorCertificateInventoryService inventoryService,
                                                       JudicialConnectorSecurityPackService packService,
                                                       JudicialConnectorCryptoAdminOpsService adminOpsService,
                                                       JudicialConnectorSecuritySessionService sessionService,
                                                       JudicialConnectorCryptographicFailureEventRepository failureRepository) {
        this.inventoryService = Objects.requireNonNull(inventoryService);
        this.packService = Objects.requireNonNull(packService);
        this.adminOpsService = Objects.requireNonNull(adminOpsService);
        this.sessionService = Objects.requireNonNull(sessionService);
        this.failureRepository = Objects.requireNonNull(failureRepository);
    }

    @Transactional(readOnly = true)
    public JudicialConnectorCryptoCommandCenterReport nationalReport(Duration recentFailureWindow) {
        return buildReport(null, recentFailureWindow);
    }

    @Transactional(readOnly = true)
    public JudicialConnectorCryptoCommandCenterReport tribunalReport(String tribunalCodigo, Duration recentFailureWindow) {
        return buildReport(normalizeCode(tribunalCodigo), recentFailureWindow);
    }

    private JudicialConnectorCryptoCommandCenterReport buildReport(String tribunalCodigo, Duration recentFailureWindow) {
        Duration window = recentFailureWindow == null || recentFailureWindow.isNegative() || recentFailureWindow.isZero() ? Duration.ofHours(24) : recentFailureWindow;
        List<JudicialConnectorCertificateInventoryReport> inventory = inventoryService.latestInventory().stream()
                .filter(report -> tribunalCodigo == null || Objects.equals(normalizeCode(report.tribunalCodigo()), tribunalCodigo))
                .toList();
        List<JudicialConnectorSecurityPackReport> packs = packService.effectivePacks().stream()
                .filter(report -> tribunalCodigo == null || Objects.equals(normalizeCode(report.tribunalCodigo()), tribunalCodigo))
                .toList();
        JudicialConnectorSecurityPackSummary packSummary = summarizePacks(packs);
        Instant threshold = Instant.now().minus(window);
        List<JudicialConnectorSecuritySessionReport> recentSessions = sessionService.recentSessions(window, tribunalCodigo);
        JudicialConnectorSecuritySessionSummary sessionSummary = sessionService.summary(window, tribunalCodigo);
        List<Map<String, Object>> recentFailures = failureRepository.findTop200ByCreatedAtAfterOrderByCreatedAtDesc(threshold).stream()
                .filter(event -> tribunalCodigo == null || Objects.equals(normalizeCode(event.getTribunalCodigo()), tribunalCodigo))
                .map(this::toFailureMap)
                .toList();
        List<Map<String, Object>> recentAdminOperations = adminOpsService.recentOperations().stream()
                .filter(item -> tribunalCodigo == null || Objects.equals(normalizeCode(asText(item.get("tribunalCodigo"))), tribunalCodigo))
                .toList();
        JudicialConnectorCryptoPostureSummary postureSummary = summarizePosture(inventory, recentFailures, window);
        LinkedHashSet<String> alerts = new LinkedHashSet<>();
        if (postureSummary.blockedCount() > 0) {
            alerts.add("CRYPTO_POSTURE_BLOCKED_TARGETS_PRESENT");
        }
        if (postureSummary.expiredCount() > 0) {
            alerts.add("CRYPTO_CERTIFICATE_EXPIRED_TARGETS_PRESENT");
        }
        if (postureSummary.expiringSoonCount() > 0) {
            alerts.add("CRYPTO_CERTIFICATE_EXPIRING_SOON_TARGETS_PRESENT");
        }
        if (!recentFailures.isEmpty()) {
            alerts.add("CRYPTO_RECENT_FAILURES_PRESENT");
        }
        if (sessionSummary.transportFailureCount() > 0) {
            alerts.add("CRYPTO_RECENT_TRANSPORT_FAILURES_PRESENT");
        }
        if (sessionSummary.remoteFailureCount() > 0) {
            alerts.add("CRYPTO_RECENT_REMOTE_FAILURES_PRESENT");
        }
        if (packs.stream().anyMatch(report -> report.tlsMode() != JudicialConnectorTlsMode.MTLS)) {
            alerts.add("CRYPTO_NON_MTLS_TARGETS_PRESENT");
        }
        if (packs.stream().anyMatch(report -> !report.hostnameVerification())) {
            alerts.add("CRYPTO_HOSTNAME_VERIFICATION_DISABLED_PRESENT");
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tribunalFilter", tribunalCodigo);
        metadata.put("inventoryCount", inventory.size());
        metadata.put("packCount", packs.size());
        metadata.put("failureCount", recentFailures.size());
        metadata.put("sessionCount", recentSessions.size());
        metadata.put("adminOperationCount", recentAdminOperations.size());
        metadata.put("recentFailureWindowSeconds", window.toSeconds());
        return new JudicialConnectorCryptoCommandCenterReport(
                Instant.now(),
                tribunalCodigo,
                postureSummary,
                packSummary,
                packs,
                inventory,
                sessionSummary,
                recentSessions,
                recentFailures,
                recentAdminOperations,
                List.copyOf(alerts),
                Map.copyOf(JudicialMapSupport.copyNonNull(metadata))
        );
    }


    private JudicialConnectorCryptoPostureSummary summarizePosture(List<JudicialConnectorCertificateInventoryReport> inventory,
                                                                   List<Map<String, Object>> recentFailures,
                                                                   Duration window) {
        int valid = 0;
        int warning = 0;
        int blocked = 0;
        int expired = 0;
        int expiringSoon = 0;
        int hardwareBacked = 0;
        ArrayList<Map<String, Object>> blockedTargets = new ArrayList<>();
        ArrayList<Map<String, Object>> expiringTargets = new ArrayList<>();
        for (JudicialConnectorCertificateInventoryReport report : inventory) {
            String status = normalizeCode(report.validationStatus());
            if ("VALID".equals(status)) {
                valid++;
            } else if ("WARNINGS".equals(status) || "WARNING".equals(status)) {
                warning++;
            } else {
                blocked++;
            }
            if (report.expired()) {
                expired++;
            }
            if (report.expiresSoon()) {
                expiringSoon++;
            }
            if (report.hardwareBacked()) {
                hardwareBacked++;
            }
            if (!"VALID".equals(status)) {
                blockedTargets.add(JudicialMapSupport.compact(
                        "system", report.system() != null ? report.system().name() : null,
                        "tribunalCodigo", report.tribunalCodigo(),
                        "bindingId", report.bindingId(),
                        "validationStatus", report.validationStatus()
                ));
            }
            if (report.expired() || report.expiresSoon()) {
                expiringTargets.add(JudicialMapSupport.compact(
                        "system", report.system() != null ? report.system().name() : null,
                        "tribunalCodigo", report.tribunalCodigo(),
                        "bindingId", report.bindingId(),
                        "notAfter", report.notAfter() != null ? report.notAfter().toString() : null
                ));
            }
        }
        return new JudicialConnectorCryptoPostureSummary(
                Instant.now(),
                inventory.size(),
                valid,
                warning,
                blocked,
                expired,
                expiringSoon,
                hardwareBacked,
                recentFailures.size(),
                List.copyOf(blockedTargets),
                List.copyOf(expiringTargets),
                Map.of("recentFailureWindowSeconds", window.toSeconds())
        );
    }

    private JudicialConnectorSecurityPackSummary summarizePacks(List<JudicialConnectorSecurityPackReport> packs) {
        int mutualTlsPacks = 0;
        int hostnameVerifiedPacks = 0;
        int revocationEnforcedPacks = 0;
        int tribunalScopedPacks = 0;
        int hardwareBoundKeyStoreReferences = 0;
        ArrayList<Map<String, Object>> targets = new ArrayList<>();
        for (JudicialConnectorSecurityPackReport pack : packs) {
            if (pack.tlsMode() == JudicialConnectorTlsMode.MTLS) {
                mutualTlsPacks++;
            }
            if (pack.hostnameVerification()) {
                hostnameVerifiedPacks++;
            }
            if (pack.revocationMode() != null && pack.revocationMode() != JudicialCertificateRevocationMode.DISABLED) {
                revocationEnforcedPacks++;
            }
            if (pack.tribunalCodigo() != null) {
                tribunalScopedPacks++;
            }
            if (pack.keyStoreRef() != null && pack.keyStoreRef().toUpperCase(Locale.ROOT).contains("PKCS11")) {
                hardwareBoundKeyStoreReferences++;
            }
            targets.add(JudicialMapSupport.compact(
                    "system", pack.system() != null ? pack.system().name() : null,
                    "tribunalCodigo", pack.tribunalCodigo(),
                    "tlsMode", pack.tlsMode() != null ? pack.tlsMode().name() : null,
                    "packId", pack.packId()
            ));
        }
        return new JudicialConnectorSecurityPackSummary(
                Instant.now(),
                packs.size(),
                mutualTlsPacks,
                hostnameVerifiedPacks,
                revocationEnforcedPacks,
                tribunalScopedPacks,
                hardwareBoundKeyStoreReferences,
                List.copyOf(targets),
                Map.of("commandCenter", true)
        );
    }

    private Map<String, Object> toFailureMap(JudicialConnectorCryptographicFailureEvent event) {
        return JudicialMapSupport.compact(
                "id", event.getId() != null ? event.getId().toString() : null,
                "system", event.getConnectorSystem() != null ? event.getConnectorSystem().name() : null,
                "tribunalCodigo", event.getTribunalCodigo(),
                "environmentName", event.getEnvironmentName(),
                "operationName", event.getOperationName(),
                "failureType", event.getFailureType() != null ? event.getFailureType().name() : null,
                "failureCode", event.getFailureCode(),
                "failureFingerprint", event.getFailureFingerprint(),
                "sanitizedMessage", event.getSanitizedMessage(),
                "keyAlias", event.getKeyAlias(),
                "keyStoreRef", event.getKeyStoreRef(),
                "trustStoreRef", event.getTrustStoreRef(),
                "tlsMode", event.getTlsMode(),
                "createdAt", event.getCreatedAt() != null ? event.getCreatedAt().toString() : null
        );
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeCode(String value) {
        String text = asText(value);
        return text == null ? null : text.toUpperCase(Locale.ROOT);
    }
}
