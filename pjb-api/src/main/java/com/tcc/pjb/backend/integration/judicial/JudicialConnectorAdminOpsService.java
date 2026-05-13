package com.tcc.pjb.backend.integration.judicial;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorAdminOperation;
import com.tcc.pjb.backend.model.repository.JudicialConnectorAdminOperationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JudicialConnectorAdminOpsService {

    private final JudicialConnectorPolicyService policyService;
    private final JudicialConnectorAdminOperationRepository repository;
    private final JudicialConnectorControlPlaneService controlPlaneService;
    private final JudicialConnectorDataPlaneService dataPlaneService;
    private final ObjectMapper objectMapper;

    public JudicialConnectorAdminOpsService(JudicialConnectorPolicyService policyService,
                                            JudicialConnectorAdminOperationRepository repository,
                                            JudicialConnectorControlPlaneService controlPlaneService,
                                            JudicialConnectorDataPlaneService dataPlaneService,
                                            ObjectMapper objectMapper) {
        this.policyService = Objects.requireNonNull(policyService);
        this.repository = Objects.requireNonNull(repository);
        this.controlPlaneService = Objects.requireNonNull(controlPlaneService);
        this.dataPlaneService = Objects.requireNonNull(dataPlaneService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public JudicialConnectorAdminOperationReport execute(JudicialConnectorAdminOperationRequest request) {
        Objects.requireNonNull(request);
        String operationType = normalizeOperationType(request.operationType());
        JudicialConnectorPolicyOverlay policy = applyMutation(operationType, request);
        JudicialConnectorControlPlaneReport controlPlane = controlPlaneService.tribunalReport(request.tribunalCodigo());
        JudicialConnectorDataPlaneReport dataPlane = dataPlaneService.tribunalReport(request.tribunalCodigo(), Duration.ofHours(24));
        String outcomeStatus = policy.blockers().isEmpty() ? "APPLIED" : "APPLIED_WITH_BLOCKERS";
        String outcomeMessage = switch (operationType) {
            case "QUARANTINE", "UNQUARANTINE" -> "Connector quarantine updated";
            case "MAINTENANCE" -> "Connector maintenance mode updated";
            case "APPROVE" -> "Connector approval policy updated";
            case "BLOCK" -> "Connector block policy updated";
            case "PATH_OVERRIDE" -> "Connector effective paths updated";
            case "ROLLOUT" -> "Connector rollout policy updated";
            default -> "Connector policy updated";
        };
        JudicialConnectorAdminOperation operation = new JudicialConnectorAdminOperation();
        operation.setConnectorSystem(request.system());
        operation.setTribunalCodigo(normalizeCode(request.tribunalCodigo()));
        operation.setEnvironmentName(normalizeEnvironment(request.environmentName()));
        operation.setOperationType(operationType);
        operation.setRequestedBy(trim(request.requestedBy()));
        operation.setReason(trim(request.reason()));
        operation.setPayloadJson(writePayload(request));
        operation.setOutcomeStatus(outcomeStatus);
        operation.setOutcomeMessage(outcomeMessage);
        repository.save(operation);
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        if (trim(request.requestedBy()) != null) metadata.put("requestedBy", trim(request.requestedBy()));
        return new JudicialConnectorAdminOperationReport(Instant.now(), operationType, request.system(), normalizeCode(request.tribunalCodigo()), policy.environmentName(), outcomeStatus, outcomeMessage, policy, controlPlane, dataPlane, recentOperations(), Map.copyOf(metadata));
    }

    public List<Map<String, Object>> recentOperations() {
        return repository.findTop100ByOrderByCreatedAtDesc().stream().map(this::toMap).toList();
    }

    private JudicialConnectorPolicyOverlay applyMutation(String operationType, JudicialConnectorAdminOperationRequest request) {
        return switch (operationType) {
            case "QUARANTINE" -> save(request, true, null, null, null, null, null, null, null, null);
            case "UNQUARANTINE" -> save(request, false, null, null, null, null, null, null, null, null);
            case "MAINTENANCE" -> save(request, null, request.maintenanceMode(), null, null, null, null, null, null, null);
            case "APPROVE" -> save(request, null, null, true, false, request.productionReady(), null, null, null, null);
            case "BLOCK" -> save(request, null, null, false, true, request.productionReady(), null, null, null, null);
            case "PATH_OVERRIDE" -> save(request, null, null, request.tribunalHomologated(), request.tribunalBlocked(), request.productionReady(), request.submitPath(), request.dryRunPath(), request.snapshotPath(), request.eventsPath());
            default -> save(request, request.quarantineEnabled(), request.maintenanceMode(), request.tribunalHomologated(), request.tribunalBlocked(), request.productionReady(), request.submitPath(), request.dryRunPath(), request.snapshotPath(), request.eventsPath());
        };
    }

    private JudicialConnectorPolicyOverlay save(JudicialConnectorAdminOperationRequest request,
                                                Boolean quarantineEnabled,
                                                Boolean maintenanceMode,
                                                Boolean tribunalHomologated,
                                                Boolean tribunalBlocked,
                                                Boolean productionReady,
                                                String submitPath,
                                                String dryRunPath,
                                                String snapshotPath,
                                                String eventsPath) {
        return policyService.save(new JudicialConnectorPolicyCommand(null, request.system(), request.environmentName(), request.tribunalCodigo(), true, productionReady, tribunalHomologated, tribunalBlocked, quarantineEnabled, maintenanceMode, request.contractVersion(), request.certificateAlias(), submitPath, dryRunPath, snapshotPath, eventsPath, request.rolloutState(), request.requestedBy(), request.reason(), request.notes(), request.validFrom(), request.validUntil(), request.metadata()));
    }

    private Map<String, Object> toMap(JudicialConnectorAdminOperation operation) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("id", operation.getId() != null ? operation.getId().toString() : null);
        out.put("system", operation.getConnectorSystem() != null ? operation.getConnectorSystem().name() : null);
        out.put("tribunalCodigo", operation.getTribunalCodigo());
        out.put("environmentName", operation.getEnvironmentName());
        out.put("operationType", operation.getOperationType());
        out.put("requestedBy", operation.getRequestedBy());
        out.put("reason", operation.getReason());
        out.put("outcomeStatus", operation.getOutcomeStatus());
        out.put("outcomeMessage", operation.getOutcomeMessage());
        out.put("createdAt", operation.getCreatedAt() != null ? operation.getCreatedAt().toString() : null);
        out.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(out);
    }

    private String writePayload(JudicialConnectorAdminOperationRequest request) {
        try { return objectMapper.writeValueAsString(request); } catch (JsonProcessingException e) { return "{\"serializationError\":\"" + e.getClass().getSimpleName() + "\"}"; }
    }
    private String normalizeOperationType(String value) { String n = trim(value); return n == null ? "UPSERT" : n.toUpperCase(Locale.ROOT); }
    private String normalizeCode(String value) { String n = trim(value); return n == null ? null : n.toUpperCase(Locale.ROOT); }
    private String normalizeEnvironment(String value) { String n = trim(value); return n == null ? null : n.toUpperCase(Locale.ROOT); }
    private String trim(String value) { if (value == null) return null; String n = value.trim(); return n.isBlank() ? null : n; }
}
