package com.tcc.pjb.backend.integration.judicial;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.model.entity.Processo;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class JudicialProtocolSubmissionService {

    private final JudicialConnectorRegistry judicialConnectorRegistry;
    private final ObjectMapper objectMapper;
    private final JudicialConnectorTelemetryService telemetryService;
    private final JudicialConnectorReadinessService readinessService;
    private final JudicialConnectorHomologationService homologationService;
    private final JudicialConnectorOperationalProfileService operationalProfileService;

    public JudicialProtocolSubmissionService(JudicialConnectorRegistry judicialConnectorRegistry,
                                             ObjectMapper objectMapper,
                                             JudicialConnectorTelemetryService telemetryService,
                                             JudicialConnectorReadinessService readinessService,
                                             JudicialConnectorHomologationService homologationService,
                                             JudicialConnectorOperationalProfileService operationalProfileService) {
        this.judicialConnectorRegistry = Objects.requireNonNull(judicialConnectorRegistry);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.telemetryService = Objects.requireNonNull(telemetryService);
        this.readinessService = Objects.requireNonNull(readinessService);
        this.homologationService = Objects.requireNonNull(homologationService);
        this.operationalProfileService = Objects.requireNonNull(operationalProfileService);
    }

    public Optional<ProtocolSubmissionResult> submitIfEligible(Processo processo,
                                                               ProceduralSubmissionBlueprintReport blueprint,
                                                               ProceduralConnectorExecutionReport execution,
                                                               boolean autoProtocolRequested) {
        if (!autoProtocolRequested || processo == null || blueprint == null || execution == null) {
            return Optional.empty();
        }
        if (!execution.allowedToAutoSubmit() || !blueprint.readyForRealConnectorSubmission() || blueprint.judicialSystem() == null) {
            return Optional.empty();
        }
        JudicialProcessConnector connector = judicialConnectorRegistry.find(blueprint.judicialSystem())
                .orElseGet(() -> judicialConnectorRegistry.get(JudicialSystem.OUTRO));
        ProtocolSubmissionRequest request = buildRequest(processo, blueprint, execution);
        JudicialConnectorHomologationReport homologation = homologationService.analyze(blueprint.judicialSystem(), connector.capability(), request);
        request = homologationService.apply(request, homologation);
        JudicialConnectorReadinessReport readiness = readinessService.analyze(blueprint.judicialSystem(), connector.capability(), request);
        JudicialConnectorOperationalProfileReport operationalProfile = operationalProfileService.analyze(blueprint.judicialSystem(), connector.capability(), request);
        request = enrichRequestMetadata(request, readiness, operationalProfile);
        if (!readiness.readyForSubmission()) {
            return Optional.of(new ProtocolSubmissionResult(
                    false,
                    blueprint.judicialSystem(),
                    null,
                    "CONNECTOR_READINESS_BLOCKED",
                    readiness.blockers().isEmpty() ? "Conector judicial sem prontidão operacional." : String.join(" | ", readiness.blockers()),
                    java.time.Instant.now(),
                    JudicialMapSupport.compact("readiness", readiness.toMap(), "operationalProfile", operationalProfile.toMap())
            ));
        }
        telemetryService.recordSubmissionRequest(processo, request, blueprint, execution);
        ProtocolSubmissionResult result = connector.submit(request);
        telemetryService.recordSubmissionResult(processo, request, result);
        return Optional.of(result);
    }

    public ProtocolSubmissionRequest buildRequest(Processo processo,
                                                  ProceduralSubmissionBlueprintReport blueprint,
                                                  ProceduralConnectorExecutionReport execution) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("processoId", processo != null ? processo.getId() : null);
        metadata.put("executionMode", execution != null ? execution.executionMode() : null);
        metadata.put("submissionLane", execution != null ? execution.submissionLane() : null);
        metadata.put("targetKey", execution != null ? execution.tribunalTargetKey() : null);
        metadata.put("connectorChecklist", execution != null ? execution.executionChecklist() : null);
        metadata.put("requestPreview", blueprint != null ? blueprint.protocolRequestPreview() : Map.of());
        metadata.put("idempotencyKey", execution != null ? execution.idempotencyKey() : null);
        metadata.put("signerMode", execution != null ? execution.signerMode() : null);
        metadata.put("retryPolicy", execution != null ? execution.retryPolicy() : null);
        metadata.put("requiresHumanReview", execution != null && execution.requiresHumanReview());
        metadata.put("requestedAt", LocalDateTime.now().toString());
        metadata.put("judicialSystem", blueprint != null && blueprint.judicialSystem() != null ? blueprint.judicialSystem().name() : null);
        metadata.put("requiresGovBrStepUp", blueprint != null && blueprint.requiresGovBrStepUp());
        metadata.put("requiresCertificate", blueprint != null && blueprint.requiresCertificate());
        metadata.put("executionReport", execution != null ? execution.toMap() : null);
        metadata.put("blueprintStatus", blueprint != null ? blueprint.blueprintStatus() : null);
        if (processo != null && processo.getConnectorSystem() != null) {
            metadata.put("connectorSystem", processo.getConnectorSystem());
        }
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        ProtocolSubmissionRequest request = new ProtocolSubmissionRequest(
                finalizeRequestId(blueprint != null ? blueprint.requestId() : null, processo != null ? processo.getId() : null),
                firstNonBlank(processo != null ? processo.getNumeroUnificado() : null, processo != null ? processo.getNumeroProcesso() : null),
                firstNonBlank(processo != null ? processo.getPedidoPrincipal() : null, processo != null ? processo.getAssunto() : null, blueprint != null ? blueprint.classeTpuNome() : null),
                blueprint != null ? blueprint.tribunalCodigo() : null,
                blueprint != null ? blueprint.unidadeJudiciariaCodigo() : null,
                blueprint != null ? blueprint.unidadeJudiciariaNome() : null,
                blueprint != null ? blueprint.rito() : null,
                blueprint != null ? blueprint.classeTpuCodigo() : null,
                processo != null && processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null,
                serializePreview(blueprint != null ? blueprint.protocolRequestPreview() : Map.of()),
                processo != null ? processo.getMaterialProbatorioHash() : null,
                processo != null && processo.getUsuario() != null ? processo.getUsuario().getId() : null,
                processo != null && processo.getUsuario() != null ? processo.getUsuario().getId() : null,
                false,
                Map.copyOf(metadata)
        );
        JudicialConnectorOperationalProfileReport operationalProfile = operationalProfileService.analyze(
                blueprint != null ? blueprint.judicialSystem() : JudicialSystem.OUTRO,
                request
        );
        LinkedHashMap<String, Object> enrichedMetadata = new LinkedHashMap<>(request.metadata());
        enrichedMetadata.put("connectorOperationalProfile", operationalProfile.toMap());
        return new ProtocolSubmissionRequest(
                request.requestId(),
                request.numeroUnificado(),
                request.title(),
                request.tribunalCodigo(),
                request.unidadeJudiciariaCodigo(),
                request.unidadeJudiciariaNome(),
                request.rito(),
                request.classeTpu(),
                request.ramoDireito(),
                request.payloadJson(),
                request.integrityHash(),
                request.signerUserId(),
                request.executorUserId(),
                request.dryRun(),
                Map.copyOf(enrichedMetadata)
        );
    }

    private ProtocolSubmissionRequest enrichRequestMetadata(ProtocolSubmissionRequest request,
                                                          JudicialConnectorReadinessReport readiness,
                                                          JudicialConnectorOperationalProfileReport operationalProfile) {
        if (request == null) {
            return null;
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(request.metadata());
        if (readiness != null) {
            metadata.put("connectorReadiness", readiness.toMap());
        }
        if (operationalProfile != null) {
            metadata.put("connectorOperationalProfile", operationalProfile.toMap());
        }
        return new ProtocolSubmissionRequest(
                request.requestId(),
                request.numeroUnificado(),
                request.title(),
                request.tribunalCodigo(),
                request.unidadeJudiciariaCodigo(),
                request.unidadeJudiciariaNome(),
                request.rito(),
                request.classeTpu(),
                request.ramoDireito(),
                request.payloadJson(),
                request.integrityHash(),
                request.signerUserId(),
                request.executorUserId(),
                request.dryRun(),
                Map.copyOf(metadata)
        );
    }

    public void applySubmissionResult(Processo processo, ProtocolSubmissionResult result) {
        if (processo == null || result == null) {
            return;
        }
        processo.setConnectorSubmissionStatus(result.status());
        processo.setConnectorProtocolReference(result.protocolReference());
        processo.setConnectorSubmissionMessage(result.message());
        processo.setConnectorSubmissionAttempts((processo.getConnectorSubmissionAttempts() == null ? 0 : processo.getConnectorSubmissionAttempts()) + 1);
        processo.setConnectorLastSubmissionAttemptAt(LocalDateTime.now(ZoneOffset.UTC));
        if (result.processedAt() != null) {
            processo.setConnectorSubmissionProcessedAt(LocalDateTime.ofInstant(result.processedAt(), ZoneOffset.UTC));
        }
        if (result.system() != null) {
            processo.setConnectorSystem(firstNonBlank(processo.getConnectorSystem(), result.system().name()));
        }
        if (result.accepted()) {
            processo.setPreProtocoloStatus(firstNonBlank("PROTOCOLO_REAL_ENVIADO", processo.getPreProtocoloStatus()));
            processo.setSubmissionBlueprintStatus(firstNonBlank("REAL_PROTOCOL_SUBMITTED", processo.getSubmissionBlueprintStatus()));
        }
    }

    private String serializePreview(Map<String, Object> preview) {
        if (preview == null || preview.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(preview);
        } catch (JsonProcessingException ignored) {
            return String.valueOf(preview);
        }
    }

    private String finalizeRequestId(String requestId, Long processId) {
        if (requestId != null && !requestId.isBlank()) {
            return requestId.endsWith("-FINAL") ? requestId : requestId + "-FINAL";
        }
        return processId == null ? "PJB-FINAL" : "PJB-" + processId + "-FINAL";
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
}
