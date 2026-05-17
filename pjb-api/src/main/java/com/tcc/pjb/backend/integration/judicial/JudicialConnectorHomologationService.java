package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import jakarta.inject.Inject;

@Service
public class JudicialConnectorHomologationService {

    private final JudicialIntegrationProperties integrationProperties;
    private final JudicialConnectorPolicyService policyService;

    public static JudicialConnectorHomologationService withoutPolicy(JudicialIntegrationProperties integrationProperties) {
        return new JudicialConnectorHomologationService(integrationProperties, null);
    }

    @Inject
    public JudicialConnectorHomologationService(JudicialIntegrationProperties integrationProperties,
                                                ObjectProvider<JudicialConnectorPolicyService> policyServiceProvider) {
        this.integrationProperties = Objects.requireNonNull(integrationProperties);
        this.policyService = policyServiceProvider == null ? null : policyServiceProvider.getIfAvailable();
    }

    public JudicialConnectorHomologationReport analyze(JudicialSystem system,
                                                       JudicialSubmissionCapability capability,
                                                       ProtocolSubmissionRequest request) {
        JudicialIntegrationProperties.Connector cfg = integrationProperties.connectorFor(system);
        String tribunalCodigo = normalizeCode(request != null ? request.tribunalCodigo() : null);
        JudicialConnectorPolicyOverlay policy = resolvePolicy(system, tribunalCodigo);
        boolean productionReady = resolveProductionReady(cfg, capability, policy);
        boolean tribunalBlocked = resolveTribunalBlocked(cfg, tribunalCodigo, policy);
        boolean tribunalHomologated = resolveTribunalHomologated(cfg, tribunalCodigo, productionReady, policy);
        boolean tribunalPolicyConflict = tribunalBlocked && tribunalHomologated;
        String effectiveSubmitPath = firstNonBlank(metadataPath(request, "connectorProtocolPath"), policy.submitPath(), lookupPath(cfg.getTribunalSubmitPaths(), tribunalCodigo), cfg.getSubmitPath());
        String effectiveDryRunPath = firstNonBlank(metadataPath(request, "connectorDryRunPath"), policy.dryRunPath(), lookupPath(cfg.getTribunalDryRunPaths(), tribunalCodigo), cfg.getDryRunPath(), effectiveSubmitPath);
        String effectiveSnapshotPath = firstNonBlank(metadataPath(request, "connectorSnapshotPath"), policy.snapshotPath(), lookupPath(cfg.getTribunalSnapshotPaths(), tribunalCodigo), cfg.getSnapshotPath());
        String effectiveEventsPath = firstNonBlank(metadataPath(request, "connectorEventsPath"), policy.eventsPath(), lookupPath(cfg.getTribunalEventsPaths(), tribunalCodigo), cfg.getEventsPath());
        boolean submitHomologated = tribunalHomologated && !tribunalBlocked && !Boolean.TRUE.equals(policy.quarantineEnabled()) && (hasText(effectiveSubmitPath) || capability != null && hasText(capability.baseUrl()));
        boolean syncHomologated = tribunalHomologated && !tribunalBlocked && !Boolean.TRUE.equals(policy.quarantineEnabled()) && (hasText(effectiveSnapshotPath) || hasText(effectiveEventsPath) || capability != null && hasText(capability.baseUrl()));
        List<String> blockers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (tribunalBlocked) blockers.add("CONNECTOR_TRIBUNAL_BLOCKED");
        if (!tribunalHomologated) blockers.add("CONNECTOR_TRIBUNAL_NOT_HOMOLOGATED");
        if (!submitHomologated) blockers.add("CONNECTOR_TRIBUNAL_SUBMIT_PATH_UNRESOLVED");
        if (Boolean.TRUE.equals(policy.quarantineEnabled())) blockers.add("CONNECTOR_POLICY_QUARANTINED");
        if (tribunalPolicyConflict) warnings.add("CONNECTOR_TRIBUNAL_POLICY_CONFLICT");
        if (!productionReady) warnings.add("CONNECTOR_ENVIRONMENT_NOT_MARKED_AS_PRODUCTION_READY");
        if (!syncHomologated) warnings.add("CONNECTOR_TRIBUNAL_SYNC_PATH_UNRESOLVED");
        if (Boolean.TRUE.equals(policy.maintenanceMode())) warnings.add("CONNECTOR_POLICY_MAINTENANCE_MODE");
        blockers.addAll(policy.blockers());
        warnings.addAll(policy.warnings());
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tribunalCodigo", tribunalCodigo);
        metadata.put("homologatedTribunals", normalizedSet(cfg.getHomologatedTribunals()));
        metadata.put("blockedTribunals", normalizedSet(cfg.getBlockedTribunals()));
        metadata.put("tribunalSubmitPaths", safeKeys(cfg.getTribunalSubmitPaths()));
        metadata.put("tribunalDryRunPaths", safeKeys(cfg.getTribunalDryRunPaths()));
        metadata.put("tribunalSnapshotPaths", safeKeys(cfg.getTribunalSnapshotPaths()));
        metadata.put("tribunalEventsPaths", safeKeys(cfg.getTribunalEventsPaths()));
        metadata.put("requestId", request != null ? request.requestId() : null);
        if (policy.policyPresent()) {
            metadata.put("policy", policy.toMap());
            metadata.put("policyCertificateAlias", policy.certificateAlias());
            metadata.put("contractVersion", policy.contractVersion());
            metadata.put("rolloutState", policy.rolloutState());
        }
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return new JudicialConnectorHomologationReport(Instant.now(), system, tribunalCodigo, productionReady, tribunalHomologated, tribunalBlocked, submitHomologated, syncHomologated, effectiveSubmitPath, effectiveDryRunPath, effectiveSnapshotPath, effectiveEventsPath, List.copyOf(deduplicate(blockers)), List.copyOf(deduplicate(warnings)), Map.copyOf(metadata));
    }

    public ProtocolSubmissionRequest apply(ProtocolSubmissionRequest request, JudicialConnectorHomologationReport report) {
        if (request == null || report == null) return request;
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(request.metadata() == null ? Map.of() : request.metadata());
        putIfHasText(metadata, "connectorProtocolPath", report.effectiveSubmitPath());
        putIfHasText(metadata, "connectorDryRunPath", report.effectiveDryRunPath());
        putIfHasText(metadata, "connectorSnapshotPath", report.effectiveSnapshotPath());
        putIfHasText(metadata, "connectorEventsPath", report.effectiveEventsPath());
        if (report.metadata() != null && report.metadata().get("policyCertificateAlias") != null) putIfHasText(metadata, "connectorCertificateAlias", String.valueOf(report.metadata().get("policyCertificateAlias")));
        metadata.put("connectorHomologation", report.toMap());
        return new ProtocolSubmissionRequest(request.requestId(), request.numeroUnificado(), request.title(), request.tribunalCodigo(), request.unidadeJudiciariaCodigo(), request.unidadeJudiciariaNome(), request.rito(), request.classeTpu(), request.ramoDireito(), request.payloadJson(), request.integrityHash(), request.signerUserId(), request.executorUserId(), request.dryRun(), Map.copyOf(metadata));
    }

    private JudicialConnectorPolicyOverlay resolvePolicy(JudicialSystem system, String tribunalCodigo) {
        return policyService == null ? JudicialConnectorPolicyOverlay.none(system, null, tribunalCodigo) : policyService.resolve(system, tribunalCodigo);
    }
    private boolean resolveProductionReady(JudicialIntegrationProperties.Connector cfg, JudicialSubmissionCapability capability, JudicialConnectorPolicyOverlay policy) {
        if (policy.policyPresent() && policy.productionReady() != null) return policy.productionReady();
        if (cfg == null) return capability != null && capability.operational();
        return cfg.isProductionReady() || capability != null && capability.operational();
    }
    private boolean resolveTribunalHomologated(JudicialIntegrationProperties.Connector cfg, String tribunalCodigo, boolean productionReady, JudicialConnectorPolicyOverlay policy) {
        if (policy.policyPresent() && policy.tribunalHomologated() != null) return policy.tribunalHomologated();
        if (cfg == null) return productionReady;
        Set<String> homologated = normalizedSet(cfg.getHomologatedTribunals());
        if (homologated.isEmpty()) return productionReady;
        return tribunalCodigo != null && homologated.contains(tribunalCodigo);
    }
    private boolean resolveTribunalBlocked(JudicialIntegrationProperties.Connector cfg, String tribunalCodigo, JudicialConnectorPolicyOverlay policy) {
        if (policy.policyPresent() && policy.tribunalBlocked() != null) return policy.tribunalBlocked();
        if (cfg == null || tribunalCodigo == null) return false;
        return normalizedSet(cfg.getBlockedTribunals()).contains(tribunalCodigo);
    }
    private String metadataPath(ProtocolSubmissionRequest request, String key) { if (request == null || request.metadata() == null || request.metadata().isEmpty() || key == null) return null; Object value = request.metadata().get(key); return value == null ? null : normalizePath(String.valueOf(value)); }
    private String lookupPath(Map<String, String> paths, String tribunalCodigo) { if (paths == null || paths.isEmpty() || tribunalCodigo == null) return null; String direct = paths.get(tribunalCodigo); if (hasText(direct)) return normalizePath(direct); for (Map.Entry<String, String> entry : paths.entrySet()) if (entry.getKey() != null && tribunalCodigo.equalsIgnoreCase(entry.getKey()) && hasText(entry.getValue())) return normalizePath(entry.getValue()); return null; }
    private Set<String> normalizedSet(List<String> values) { if (values == null || values.isEmpty()) return Set.of(); LinkedHashSet<String> normalized = new LinkedHashSet<>(); for (String value : values) { String code = normalizeCode(value); if (code != null) normalized.add(code); } return Set.copyOf(normalized); }
    private List<String> safeKeys(Map<String, String> values) { if (values == null || values.isEmpty()) return List.of(); LinkedHashSet<String> keys = new LinkedHashSet<>(); values.keySet().forEach(key -> { String normalized = normalizeCode(key); if (normalized != null) keys.add(normalized); }); return List.copyOf(keys); }
    private List<String> deduplicate(List<String> values) { return values == null || values.isEmpty() ? List.of() : new ArrayList<>(new LinkedHashSet<>(values)); }
    private String normalizeCode(String value) { return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT); }
    private String normalizePath(String value) { if (value == null || value.isBlank()) return null; String normalized = value.trim(); return normalized.startsWith("/") ? normalized : '/' + normalized; }
    private void putIfHasText(Map<String, Object> metadata, String key, String value) { if (metadata != null && key != null && hasText(value)) metadata.put(key, value); }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String firstNonBlank(String... values) { if (values == null) return null; for (String value : values) if (value != null && !value.isBlank()) return value.trim(); return null; }
}
