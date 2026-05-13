package com.tcc.pjb.backend.integration.judicial;

import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorPolicy;
import com.tcc.pjb.backend.model.repository.JudicialConnectorPolicyRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class JudicialConnectorPolicyService {

    private final JudicialConnectorPolicyRepository repository;
    private final Environment environment;

    public JudicialConnectorPolicyService(JudicialConnectorPolicyRepository repository, Environment environment) {
        this.repository = Objects.requireNonNull(repository);
        this.environment = Objects.requireNonNull(environment);
    }

    public JudicialConnectorPolicyOverlay resolve(JudicialSystem system, String tribunalCodigo) {
        if (system == null) {
            return JudicialConnectorPolicyOverlay.none(null, currentEnvironmentName(), normalizeCode(tribunalCodigo));
        }
        String envName = currentEnvironmentName();
        String tribunal = normalizeCode(tribunalCodigo);
        List<JudicialConnectorPolicy> applicable = repository.findAllByConnectorSystemAndActiveTrueOrderByCreatedAtDesc(system).stream()
                .filter(policy -> matchesEnvironment(policy.getEnvironmentName(), envName))
                .filter(policy -> matchesTribunal(policy.getTribunalCodigo(), tribunal))
                .sorted(Comparator.comparingInt((JudicialConnectorPolicy policy) -> specificityScore(policy, envName, tribunal))
                        .thenComparing(JudicialConnectorPolicy::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        if (applicable.isEmpty()) {
            return JudicialConnectorPolicyOverlay.none(system, envName, tribunal);
        }
        Boolean productionReady = null;
        Boolean tribunalHomologated = null;
        Boolean tribunalBlocked = null;
        Boolean quarantineEnabled = null;
        Boolean maintenanceMode = null;
        String contractVersion = null;
        String certificateAlias = null;
        String submitPath = null;
        String dryRunPath = null;
        String snapshotPath = null;
        String eventsPath = null;
        String rolloutState = null;
        String approvedBy = null;
        String reason = null;
        String notes = null;
        Instant validFrom = null;
        Instant validUntil = null;
        UUID policyId = null;
        for (JudicialConnectorPolicy policy : applicable) {
            policyId = policy.getId();
            productionReady = take(policy.getProductionReady(), productionReady);
            tribunalHomologated = take(policy.getTribunalHomologated(), tribunalHomologated);
            tribunalBlocked = take(policy.getTribunalBlocked(), tribunalBlocked);
            quarantineEnabled = take(policy.getQuarantineEnabled(), quarantineEnabled);
            maintenanceMode = take(policy.getMaintenanceMode(), maintenanceMode);
            contractVersion = takeText(policy.getContractVersion(), contractVersion);
            certificateAlias = takeText(policy.getCertificateAlias(), certificateAlias);
            submitPath = takePath(policy.getSubmitPath(), submitPath);
            dryRunPath = takePath(policy.getDryRunPath(), dryRunPath);
            snapshotPath = takePath(policy.getSnapshotPath(), snapshotPath);
            eventsPath = takePath(policy.getEventsPath(), eventsPath);
            rolloutState = takeText(policy.getRolloutState(), rolloutState);
            approvedBy = takeText(policy.getApprovedBy(), approvedBy);
            reason = takeText(policy.getReason(), reason);
            notes = takeText(policy.getNotes(), notes);
            validFrom = policy.getValidFrom() != null ? policy.getValidFrom() : validFrom;
            validUntil = policy.getValidUntil() != null ? policy.getValidUntil() : validUntil;
        }
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if (Boolean.TRUE.equals(quarantineEnabled)) {
            blockers.add("CONNECTOR_POLICY_QUARANTINED");
        }
        if (Boolean.TRUE.equals(maintenanceMode)) {
            warnings.add("CONNECTOR_POLICY_MAINTENANCE_MODE");
        }
        if (validUntil != null && validUntil.isBefore(Instant.now())) {
            blockers.add("CONNECTOR_POLICY_EXPIRED");
        }
        long exactScopes = applicable.stream().filter(policy -> normalizeCode(policy.getTribunalCodigo()) != null).count();
        if (exactScopes > 1L) {
            warnings.add("CONNECTOR_POLICY_SCOPE_OVERLAP");
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("matchedPolicies", applicable.stream().map(this::toMap).toList());
        metadata.put("currentEnvironment", envName);
        return new JudicialConnectorPolicyOverlay(policyId, system, envName, tribunal, true, productionReady, tribunalHomologated, tribunalBlocked, quarantineEnabled, maintenanceMode, contractVersion, certificateAlias, submitPath, dryRunPath, snapshotPath, eventsPath, rolloutState, approvedBy, reason, notes, validFrom, validUntil, List.copyOf(blockers), List.copyOf(warnings), Map.copyOf(metadata));
    }

    public JudicialConnectorPolicyOverlay save(JudicialConnectorPolicyCommand command) {
        Objects.requireNonNull(command);
        JudicialConnectorPolicy entity = resolveTargetEntity(command);
        entity.setConnectorSystem(command.system());
        entity.setEnvironmentName(normalizeEnvironment(command.environmentName()));
        entity.setTribunalCodigo(normalizeCode(command.tribunalCodigo()));
        entity.setActive(command.active() == null || command.active());
        entity.setProductionReady(command.productionReady());
        entity.setTribunalHomologated(command.tribunalHomologated());
        entity.setTribunalBlocked(command.tribunalBlocked());
        entity.setQuarantineEnabled(command.quarantineEnabled());
        entity.setMaintenanceMode(command.maintenanceMode());
        entity.setContractVersion(trim(command.contractVersion()));
        entity.setCertificateAlias(trim(command.certificateAlias()));
        entity.setSubmitPath(normalizePath(command.submitPath()));
        entity.setDryRunPath(normalizePath(command.dryRunPath()));
        entity.setSnapshotPath(normalizePath(command.snapshotPath()));
        entity.setEventsPath(normalizePath(command.eventsPath()));
        entity.setRolloutState(trim(command.rolloutState()));
        entity.setApprovedBy(trim(command.approvedBy()));
        entity.setReason(trim(command.reason()));
        entity.setNotes(trim(command.notes()));
        entity.setValidFrom(command.validFrom());
        entity.setValidUntil(command.validUntil());
        JudicialConnectorPolicy saved = repository.save(entity);
        return resolve(saved.getConnectorSystem(), saved.getTribunalCodigo());
    }

    public JudicialConnectorPolicyReport report() {
        List<JudicialConnectorPolicy> policies = repository.findAllByActiveTrueOrderByConnectorSystemAscTribunalCodigoAscCreatedAtDesc();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        Map<String, Long> overlaps = policies.stream().collect(Collectors.groupingBy(this::scopeKey, LinkedHashMap::new, Collectors.counting()));
        overlaps.forEach((scope, count) -> { if (count > 1L) warnings.add("POLICY_SCOPE_OVERLAP:" + scope); });
        return new JudicialConnectorPolicyReport(Instant.now(), currentEnvironmentName(), policies.size(), policies.stream().map(this::toMap).toList(), List.of(), List.copyOf(warnings), Map.of("systems", policies.stream().map(JudicialConnectorPolicy::getConnectorSystem).filter(Objects::nonNull).map(Enum::name).distinct().sorted().toList()));
    }

    private JudicialConnectorPolicy resolveTargetEntity(JudicialConnectorPolicyCommand command) {
        if (command.policyId() != null) {
            return repository.findById(command.policyId()).orElseGet(JudicialConnectorPolicy::new);
        }
        String envName = normalizeEnvironment(command.environmentName());
        String tribunal = normalizeCode(command.tribunalCodigo());
        return repository.findAllByConnectorSystemAndActiveTrueOrderByCreatedAtDesc(command.system()).stream()
                .filter(policy -> Objects.equals(normalizeEnvironment(policy.getEnvironmentName()), envName))
                .filter(policy -> Objects.equals(normalizeCode(policy.getTribunalCodigo()), tribunal))
                .findFirst()
                .orElseGet(JudicialConnectorPolicy::new);
    }

    private Map<String, Object> toMap(JudicialConnectorPolicy policy) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("policyId", policy.getId() != null ? policy.getId().toString() : null);
        out.put("system", policy.getConnectorSystem() != null ? policy.getConnectorSystem().name() : null);
        out.put("environmentName", normalizeEnvironment(policy.getEnvironmentName()));
        out.put("tribunalCodigo", normalizeCode(policy.getTribunalCodigo()));
        out.put("active", policy.isActive());
        out.put("productionReady", policy.getProductionReady());
        out.put("tribunalHomologated", policy.getTribunalHomologated());
        out.put("tribunalBlocked", policy.getTribunalBlocked());
        out.put("quarantineEnabled", policy.getQuarantineEnabled());
        out.put("maintenanceMode", policy.getMaintenanceMode());
        out.put("contractVersion", policy.getContractVersion());
        out.put("certificateAlias", policy.getCertificateAlias());
        out.put("submitPath", policy.getSubmitPath());
        out.put("dryRunPath", policy.getDryRunPath());
        out.put("snapshotPath", policy.getSnapshotPath());
        out.put("eventsPath", policy.getEventsPath());
        out.put("rolloutState", policy.getRolloutState());
        out.put("approvedBy", policy.getApprovedBy());
        out.put("reason", policy.getReason());
        out.put("notes", policy.getNotes());
        out.put("validFrom", policy.getValidFrom() != null ? policy.getValidFrom().toString() : null);
        out.put("validUntil", policy.getValidUntil() != null ? policy.getValidUntil().toString() : null);
        out.put("updatedAt", policy.getUpdatedAt() != null ? policy.getUpdatedAt().toString() : null);
        out.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(out);
    }

    private String currentEnvironmentName() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return "default";
        }
        return normalizeEnvironment(activeProfiles[0]);
    }

    private boolean matchesEnvironment(String policyEnvironment, String envName) {
        String normalized = normalizeEnvironment(policyEnvironment);
        return normalized == null || normalized.equals(envName) || "ALL".equals(normalized);
    }

    private boolean matchesTribunal(String policyTribunal, String tribunal) {
        String normalized = normalizeCode(policyTribunal);
        return normalized == null || Objects.equals(normalized, tribunal);
    }

    private int specificityScore(JudicialConnectorPolicy policy, String envName, String tribunal) {
        int score = 0;
        if (Objects.equals(normalizeEnvironment(policy.getEnvironmentName()), envName)) score += 10;
        if (Objects.equals(normalizeCode(policy.getTribunalCodigo()), tribunal)) score += 20;
        if (policy.getValidUntil() != null) score += 1;
        return score;
    }

    private Boolean take(Boolean incoming, Boolean current) { return incoming != null ? incoming : current; }
    private String takeText(String incoming, String current) { return hasText(incoming) ? incoming.trim() : current; }
    private String takePath(String incoming, String current) { return hasText(incoming) ? normalizePath(incoming) : current; }
    private String scopeKey(JudicialConnectorPolicy policy) { return (policy.getConnectorSystem() != null ? policy.getConnectorSystem().name() : "OUTRO") + '|' + firstNonBlank(normalizeEnvironment(policy.getEnvironmentName()), "default") + '|' + firstNonBlank(normalizeCode(policy.getTribunalCodigo()), "GLOBAL"); }
    private String normalizeEnvironment(String value) { return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null; }
    private String normalizeCode(String value) { return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null; }
    private String normalizePath(String value) { if (!hasText(value)) return null; String normalized = value.trim(); return normalized.startsWith("/") ? normalized : '/' + normalized; }
    private String trim(String value) { return hasText(value) ? value.trim() : null; }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String firstNonBlank(String... values) { if (values == null) return null; for (String value : values) if (hasText(value)) return value.trim(); return null; }
}
