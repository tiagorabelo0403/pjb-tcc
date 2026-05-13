package com.tcc.pjb.backend.integration.judicial.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.integration.judicial.JudicialIntegrationProperties;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorCertificateInventory;
import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorCryptographicFailureEvent;
import com.tcc.pjb.backend.model.repository.JudicialConnectorCertificateInventoryRepository;
import com.tcc.pjb.backend.model.repository.JudicialConnectorCryptographicFailureEventRepository;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JudicialConnectorCertificateInventoryService {

    private final JudicialIntegrationProperties integrationProperties;
    private final JudicialConnectorSecurityProperties securityProperties;
    private final JudicialConnectorCertificateValidationService validationService;
    private final JudicialConnectorCryptographicContextService cryptographicContextService;
    private final JudicialConnectorCertificateInventoryRepository inventoryRepository;
    private final JudicialConnectorCryptographicFailureEventRepository failureRepository;
    private final JudicialConnectorSecurityPostureMetricsService postureMetricsService;
    private final ObjectMapper objectMapper;

    public JudicialConnectorCertificateInventoryService(JudicialIntegrationProperties integrationProperties,
                                                        JudicialConnectorSecurityProperties securityProperties,
                                                        JudicialConnectorCertificateValidationService validationService,
                                                        JudicialConnectorCryptographicContextService cryptographicContextService,
                                                        JudicialConnectorCertificateInventoryRepository inventoryRepository,
                                                        JudicialConnectorCryptographicFailureEventRepository failureRepository,
                                                        JudicialConnectorSecurityPostureMetricsService postureMetricsService,
                                                        ObjectMapper objectMapper) {
        this.integrationProperties = Objects.requireNonNull(integrationProperties);
        this.securityProperties = Objects.requireNonNull(securityProperties);
        this.validationService = Objects.requireNonNull(validationService);
        this.cryptographicContextService = Objects.requireNonNull(cryptographicContextService);
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository);
        this.failureRepository = Objects.requireNonNull(failureRepository);
        this.postureMetricsService = Objects.requireNonNull(postureMetricsService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public List<JudicialConnectorCertificateInventoryReport> refreshConfiguredInventory() {
        ArrayList<JudicialConnectorCertificateInventoryReport> reports = new ArrayList<>();
        for (InventoryTarget target : configuredTargets()) {
            try {
                reports.add(refresh(target.system(), target.tribunalCodigo()));
            } catch (RuntimeException ex) {
                reports.add(failedReport(target, ex));
            }
        }
        postureMetricsService.publish(reports);
        return List.copyOf(reports);
    }

    @Transactional
    public JudicialConnectorCertificateInventoryReport refresh(JudicialSystem system, String tribunalCodigo) {
        JudicialSystem resolvedSystem = system == null ? JudicialSystem.OUTRO : system;
        String normalizedTribunal = normalizeCode(tribunalCodigo);
        JudicialIntegrationProperties.Connector connector = integrationProperties.connectorFor(resolvedSystem);
        URI targetUri = normalizeUri(connector.getBaseUrl());
        JudicialCertificateValidationReport validation = validationService.validate(
                resolvedSystem,
                normalizedTribunal,
                targetUri,
                connector,
                Map.of("inventoryRefresh", true)
        );
        JudicialResolvedSecurityBinding binding = cryptographicContextService.resolveBinding(
                resolvedSystem,
                normalizedTribunal,
                targetUri,
                connector,
                Map.of("inventoryRefresh", true)
        );
        JudicialConnectorCertificateInventory entity = inventoryRepository
                .findByConnectorSystemAndTribunalCodigoAndEnvironmentNameAndBindingId(
                        resolvedSystem,
                        normalizedTribunal,
                        firstNonBlank(validation.environmentName(), securityProperties.getEnvironmentName()),
                        firstNonBlank(validation.bindingId(), binding.bindingId(), "DEFAULT")
                )
                .orElseGet(JudicialConnectorCertificateInventory::new);
        entity.setConnectorSystem(resolvedSystem);
        entity.setTribunalCodigo(normalizedTribunal);
        entity.setEnvironmentName(firstNonBlank(validation.environmentName(), securityProperties.getEnvironmentName()));
        entity.setBindingId(firstNonBlank(validation.bindingId(), binding.bindingId(), "DEFAULT"));
        entity.setTargetUri(targetUri != null ? targetUri.toString() : null);
        entity.setKeyStoreRef(validation.keyStoreRef());
        entity.setTrustStoreRef(validation.trustStoreRef());
        entity.setKeyAlias(validation.keyAlias());
        entity.setTlsMode(binding.tlsMode() != null ? binding.tlsMode().name() : null);
        entity.setCertificatePresent(validation.certificatePresent());
        entity.setHardwareBacked(validation.hardwareBacked());
        entity.setValidNow(validation.validNow());
        entity.setExpiresSoon(validation.expiresSoon());
        entity.setExpired(validation.expired());
        entity.setTrustStorePresent(validation.trustStorePresent());
        entity.setPathValidationSucceeded(validation.pathValidationSucceeded());
        entity.setRevocationAttempted(validation.revocationAttempted());
        entity.setRevocationSoftFailed(validation.revocationSoftFailed());
        entity.setRevocationHardFailed(validation.revocationHardFailed());
        entity.setValidationStatus(firstNonBlank(validation.status(), "UNKNOWN"));
        entity.setNotBefore(validation.notBefore());
        entity.setNotAfter(validation.notAfter());
        entity.setRemainingValiditySeconds(validation.remainingValidity() != null ? validation.remainingValidity().toSeconds() : null);
        entity.setCertificateChainLength(validation.certificateChainLength());
        entity.setSubjectDn(validation.subject());
        entity.setIssuerDn(validation.issuer());
        entity.setSerialNumberHex(validation.serialNumberHex());
        entity.setSha256Fingerprint(validation.sha256Fingerprint());
        entity.setBlockersJson(writeJson(validation.blockers()));
        entity.setWarningsJson(writeJson(validation.warnings()));
        entity.setMetadataJson(writeJson(validation.toMap()));
        entity.setLastValidatedAt(validation.validatedAt());
        JudicialConnectorCertificateInventory saved = inventoryRepository.save(entity);
        JudicialConnectorCertificateInventoryReport report = toReport(saved, validation.blockers(), validation.warnings(), validation.metadata());
        postureMetricsService.publish(inventoryRepository.findAllByOrderByConnectorSystemAscTribunalCodigoAscEnvironmentNameAscBindingIdAsc().stream()
                .map(item -> toReport(item, readStringList(item.getBlockersJson()), readStringList(item.getWarningsJson()), readMap(item.getMetadataJson())))
                .toList());
        return report;
    }

    @Transactional(readOnly = true)
    public List<JudicialConnectorCertificateInventoryReport> latestInventory() {
        List<JudicialConnectorCertificateInventoryReport> reports = inventoryRepository.findAllByOrderByConnectorSystemAscTribunalCodigoAscEnvironmentNameAscBindingIdAsc()
                .stream()
                .map(entity -> toReport(entity, readStringList(entity.getBlockersJson()), readStringList(entity.getWarningsJson()), readMap(entity.getMetadataJson())))
                .toList();
        postureMetricsService.publish(reports);
        return reports;
    }

    @Transactional(readOnly = true)
    public JudicialConnectorCryptoPostureSummary postureSummary(Duration recentFailureWindow) {
        List<JudicialConnectorCertificateInventoryReport> reports = latestInventory();
        Instant threshold = Instant.now().minus(recentFailureWindow == null ? Duration.ofHours(24) : recentFailureWindow);
        Collection<JudicialConnectorCryptographicFailureEvent> recentFailures = failureRepository.findTop200ByCreatedAtAfterOrderByCreatedAtDesc(threshold);
        LinkedHashSet<String> failureTargets = new LinkedHashSet<>();
        recentFailures.forEach(event -> failureTargets.add(identity(event.getConnectorSystem(), event.getTribunalCodigo())));
        int valid = 0;
        int warning = 0;
        int blocked = 0;
        int expired = 0;
        int expiringSoon = 0;
        int hardwareBacked = 0;
        int withRecentFailures = 0;
        ArrayList<Map<String, Object>> blockedTargets = new ArrayList<>();
        ArrayList<Map<String, Object>> expiringTargets = new ArrayList<>();
        for (JudicialConnectorCertificateInventoryReport report : reports) {
            String status = normalizeStatus(report.validationStatus());
            switch (status) {
                case "VALID" -> valid++;
                case "WARNING", "WARNINGS" -> warning++;
                default -> blocked++;
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
            if (failureTargets.contains(identity(report.system(), report.tribunalCodigo()))) {
                withRecentFailures++;
            }
            if (!"VALID".equals(status)) {
                blockedTargets.add(minimalTarget(report));
            }
            if (report.expiresSoon() || report.expired()) {
                expiringTargets.add(minimalTarget(report));
            }
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("recentFailureWindowSeconds", (recentFailureWindow == null ? Duration.ofHours(24) : recentFailureWindow).toSeconds());
        metadata.put("recentFailureEvents", recentFailures.size());
        metadata.put("environmentName", securityProperties.getEnvironmentName());
        metadata.put("bindingsConfigured", securityProperties.getBindings().size());
        return new JudicialConnectorCryptoPostureSummary(
                Instant.now(),
                reports.size(),
                valid,
                warning,
                blocked,
                expired,
                expiringSoon,
                hardwareBacked,
                withRecentFailures,
                List.copyOf(blockedTargets),
                List.copyOf(expiringTargets),
                Map.copyOf(metadata)
        );
    }

    private List<InventoryTarget> configuredTargets() {
        LinkedHashSet<InventoryTarget> targets = new LinkedHashSet<>();
        securityProperties.getBindings().values().forEach(binding -> {
            JudicialSystem system = parseSystem(binding.getSystem());
            if (system != null && binding.isEnabled()) {
                targets.add(new InventoryTarget(system, normalizeCode(binding.getTribunalCodigo())));
            }
        });
        for (JudicialSystem system : JudicialSystem.values()) {
            if (system == JudicialSystem.OUTRO) {
                continue;
            }
            JudicialIntegrationProperties.Connector connector = integrationProperties.connectorFor(system);
            if (connector != null && connector.isEnabled() && targets.stream().noneMatch(target -> target.system() == system)) {
                targets.add(new InventoryTarget(system, null));
            }
        }
        return List.copyOf(targets);
    }


    private JudicialConnectorCertificateInventoryReport failedReport(InventoryTarget target, RuntimeException ex) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("inventoryRefreshFailure", true);
        metadata.put("errorClass", ex.getClass().getName());
        metadata.put("message", trim(ex.getMessage()));
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return new JudicialConnectorCertificateInventoryReport(
                Instant.now(),
                target.system(),
                target.tribunalCodigo(),
                securityProperties.getEnvironmentName(),
                "UNRESOLVED",
                null,
                null,
                null,
                null,
                null,
                "BLOCKED",
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                null,
                List.of("INVENTORY_REFRESH_FAILED"),
                List.of(),
                Map.copyOf(metadata)
        );
    }

    private JudicialConnectorCertificateInventoryReport toReport(JudicialConnectorCertificateInventory entity,
                                                                 List<String> blockers,
                                                                 List<String> warnings,
                                                                 Map<String, Object> metadata) {
        return new JudicialConnectorCertificateInventoryReport(
                entity.getLastValidatedAt(),
                entity.getConnectorSystem(),
                entity.getTribunalCodigo(),
                entity.getEnvironmentName(),
                entity.getBindingId(),
                entity.getTargetUri(),
                entity.getKeyStoreRef(),
                entity.getTrustStoreRef(),
                entity.getKeyAlias(),
                entity.getTlsMode(),
                entity.getValidationStatus(),
                entity.isCertificatePresent(),
                entity.isHardwareBacked(),
                entity.isValidNow(),
                entity.isExpiresSoon(),
                entity.isExpired(),
                entity.isTrustStorePresent(),
                entity.isPathValidationSucceeded(),
                entity.isRevocationAttempted(),
                entity.isRevocationSoftFailed(),
                entity.isRevocationHardFailed(),
                entity.getNotBefore(),
                entity.getNotAfter(),
                entity.getRemainingValiditySeconds(),
                entity.getCertificateChainLength(),
                entity.getSubjectDn(),
                entity.getIssuerDn(),
                entity.getSerialNumberHex(),
                entity.getSha256Fingerprint(),
                blockers,
                warnings,
                metadata
        );
    }

    private Map<String, Object> minimalTarget(JudicialConnectorCertificateInventoryReport report) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("system", report.system() != null ? report.system().name() : null);
        out.put("tribunalCodigo", report.tribunalCodigo());
        out.put("bindingId", report.bindingId());
        out.put("validationStatus", report.validationStatus());
        out.put("expiresSoon", report.expiresSoon());
        out.put("expired", report.expired());
        out.put("lastValidatedAt", report.lastValidatedAt() != null ? report.lastValidatedAt().toString() : null);
        out.put("remainingValiditySeconds", report.remainingValiditySeconds());
        out.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(out);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readerForListOf(String.class).readValue(json);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readerForMapOf(Object.class).readValue(json);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private URI normalizeUri(String value) {
        String trimmed = trim(value);
        return trimmed == null ? null : URI.create(trimmed);
    }

    private JudicialSystem parseSystem(String value) {
        String trimmed = trim(value);
        if (trimmed == null) {
            return null;
        }
        try {
            return JudicialSystem.valueOf(trimmed.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String normalizeStatus(String value) {
        String trimmed = trim(value);
        return trimmed == null ? "UNKNOWN" : trimmed.toUpperCase(Locale.ROOT);
    }

    private String identity(JudicialSystem system, String tribunalCodigo) {
        return (system == null ? JudicialSystem.OUTRO.name() : system.name()) + '|' + normalizeCode(tribunalCodigo);
    }

    private String normalizeCode(String value) {
        String trimmed = trim(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String out = value.trim();
        return out.isBlank() ? null : out;
    }

    private record InventoryTarget(JudicialSystem system, String tribunalCodigo) {
    }
}
