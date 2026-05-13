package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JudicialConnectorReadinessService {

    private final JudicialIntegrationProperties integrationProperties;
    private final JudicialConnectorHomologationService homologationService;
    private final JudicialOAuthTokenService judicialOAuthTokenService;

    public JudicialConnectorReadinessService(JudicialIntegrationProperties integrationProperties,
                                             JudicialConnectorHomologationService homologationService,
                                             JudicialOAuthTokenService judicialOAuthTokenService) {
        this.integrationProperties = Objects.requireNonNull(integrationProperties);
        this.homologationService = Objects.requireNonNull(homologationService);
        this.judicialOAuthTokenService = Objects.requireNonNull(judicialOAuthTokenService);
    }

    public JudicialConnectorReadinessReport analyze(JudicialSystem system, JudicialSubmissionCapability capability, ProtocolSubmissionRequest request) {
        Objects.requireNonNull(system, "system");
        JudicialIntegrationProperties.Connector cfg = integrationProperties.connectorFor(system);
        JudicialConnectorHomologationReport homologation = homologationService.analyze(system, capability, request);
        List<String> blockers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        boolean enabled = capability != null && capability.enabled();
        boolean baseUrlConfigured = capability != null && hasText(capability.baseUrl());
        boolean protocolPathResolved = hasText(homologation.effectiveSubmitPath()) || baseUrlConfigured;
        boolean syncPathResolved = hasText(homologation.effectiveSnapshotPath()) || hasText(homologation.effectiveEventsPath()) || baseUrlConfigured;
        JudicialConnectorAuthMode authMode = resolveAuthMode(cfg, request);
        boolean authenticationSatisfied = authMode.satisfies(cfg != null && cfg.isAuthRequired());
        boolean certificateSatisfied = hasCertificateEvidence(cfg, request, homologation);
        if (!enabled) blockers.add("CONNECTOR_DISABLED");
        if (!baseUrlConfigured) blockers.add("CONNECTOR_BASE_URL_MISSING");
        if (!protocolPathResolved) blockers.add("CONNECTOR_PROTOCOL_PATH_UNRESOLVED");
        if (!authenticationSatisfied) blockers.add("CONNECTOR_AUTH_MISSING");
        if (!certificateSatisfied) blockers.add("CONNECTOR_CERTIFICATE_EVIDENCE_MISSING");
        blockers.addAll(homologation.blockers());
        if (cfg != null && cfg.isRequiresStepUpGovBr() && !hasGovBrEvidence(request)) warnings.add("STEP_UP_EVIDENCE_MISSING");
        if (capability != null && !capability.supportsDryRun()) warnings.add("CONNECTOR_DRY_RUN_UNAVAILABLE");
        if (capability != null && !capability.supportsSnapshotSync() && !capability.supportsEventSync()) warnings.add("CONNECTOR_EXTERNAL_SYNC_UNAVAILABLE"); else if (!syncPathResolved) warnings.add("CONNECTOR_SYNC_PATH_UNRESOLVED");
        warnings.addAll(homologation.warnings());
        boolean readyForDryRun = blockers.isEmpty() && capability != null && capability.supportsDryRun();
        boolean readyForSubmission = blockers.isEmpty() && capability != null && capability.operational();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("baseUrl", capability != null ? capability.baseUrl() : null);
        metadata.put("submitPath", homologation.effectiveSubmitPath());
        metadata.put("dryRunPath", homologation.effectiveDryRunPath());
        metadata.put("snapshotPath", homologation.effectiveSnapshotPath());
        metadata.put("eventsPath", homologation.effectiveEventsPath());
        metadata.put("requiresCertificate", cfg != null && cfg.isRequiresCertificate());
        metadata.put("requiresStepUpGovBr", cfg != null && cfg.isRequiresStepUpGovBr());
        metadata.put("authMode", authMode.name());
        metadata.put("requestId", requestId(request));
        metadata.put("tribunalCodigo", tribunalCodigo(request));
        metadata.put("unidadeJudiciariaCodigo", unidadeJudiciariaCodigo(request));
        metadata.put("homologation", homologation.toMap());
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return new JudicialConnectorReadinessReport(Instant.now(), system, enabled, baseUrlConfigured, protocolPathResolved, syncPathResolved, authenticationSatisfied, certificateSatisfied, readyForDryRun, readyForSubmission, List.copyOf(deduplicate(blockers)), List.copyOf(deduplicate(warnings)), Map.copyOf(metadata));
    }

    private boolean hasCertificateEvidence(JudicialIntegrationProperties.Connector cfg, ProtocolSubmissionRequest request, JudicialConnectorHomologationReport homologation) {
        if (cfg == null || !cfg.isRequiresCertificate()) return true;
        if (hasText(cfg.getCertificateAlias())) return true;
        Map<String, Object> metadata = requestMetadata(request);
        Object policyAlias = homologation != null && homologation.metadata() != null ? homologation.metadata().get("policyCertificateAlias") : null;
        return hasText(stringValue(metadata.get("certificateAlias"))) || hasText(stringValue(metadata.get("certificateSerial"))) || hasText(stringValue(metadata.get("connectorCertificateAlias"))) || hasText(stringValue(policyAlias));
    }
    private boolean hasGovBrEvidence(ProtocolSubmissionRequest request) { Map<String, Object> metadata = requestMetadata(request); return isTrue(metadata.get("govbrStepUp")) || isTrue(metadata.get("govbr_step_up")) || isTrue(metadata.get("stepUpGovBr")); }
    private boolean hasConnectorHeaderMetadata(Map<String, Object> metadata) { if (metadata == null || metadata.isEmpty()) return false; Object headers = metadata.get("connectorHeaders"); return headers instanceof Map<?, ?> map && !map.isEmpty(); }
    private JudicialConnectorAuthMode resolveAuthMode(JudicialIntegrationProperties.Connector cfg, ProtocolSubmissionRequest request) {
        if (cfg == null) return JudicialConnectorAuthMode.NONE;
        if (hasText(cfg.getBearerToken())) return JudicialConnectorAuthMode.BEARER;
        if (hasText(cfg.getApiKey())) return JudicialConnectorAuthMode.API_KEY;
        if (hasText(cfg.getBasicUsername()) && hasText(cfg.getBasicPassword())) return JudicialConnectorAuthMode.BASIC;
        if (judicialOAuthTokenService.hasOAuthConfig(cfg)) return JudicialConnectorAuthMode.OAUTH2_CLIENT_CREDENTIALS;
        Map<String, Object> metadata = requestMetadata(request);
        if (hasText(stringValue(metadata.get("connectorAuthorization")))) {
            String raw = stringValue(metadata.get("connectorAuthorization")).toUpperCase(Locale.ROOT);
            if (raw.startsWith("BEARER ")) return JudicialConnectorAuthMode.REQUEST_BEARER;
            if (raw.startsWith("BASIC ")) return JudicialConnectorAuthMode.REQUEST_BASIC;
            return JudicialConnectorAuthMode.REQUEST_CUSTOM;
        }
        if (hasText(stringValue(metadata.get("connectorApiKey")))) return JudicialConnectorAuthMode.REQUEST_API_KEY;
        if (hasConnectorHeaderMetadata(metadata)) return JudicialConnectorAuthMode.REQUEST_HEADERS;
        return cfg.isAuthRequired() ? JudicialConnectorAuthMode.MISSING : JudicialConnectorAuthMode.NONE;
    }
    private Map<String, Object> requestMetadata(ProtocolSubmissionRequest request) { return request == null || request.metadata() == null ? Map.of() : request.metadata(); }
    private String requestId(ProtocolSubmissionRequest request) { return request == null ? null : request.requestId(); }
    private String tribunalCodigo(ProtocolSubmissionRequest request) { return request == null ? null : request.tribunalCodigo(); }
    private String unidadeJudiciariaCodigo(ProtocolSubmissionRequest request) { return request == null ? null : request.unidadeJudiciariaCodigo(); }
    private List<String> deduplicate(List<String> values) { return values == null || values.isEmpty() ? List.of() : new java.util.ArrayList<>(new java.util.LinkedHashSet<>(values)); }
    private boolean isTrue(Object value) { if (value == null) return false; if (value instanceof Boolean bool) return bool; String text = String.valueOf(value).trim(); return "true".equalsIgnoreCase(text) || "1".equals(text) || "sim".equalsIgnoreCase(text); }
    private String stringValue(Object value) { if (value == null) return null; String text = String.valueOf(value).trim(); return text.isBlank() ? null : text; }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }
}
