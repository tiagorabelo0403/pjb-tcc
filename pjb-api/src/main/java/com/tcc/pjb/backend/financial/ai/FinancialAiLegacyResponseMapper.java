package com.tcc.pjb.backend.financial.ai;

import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class FinancialAiLegacyResponseMapper {

    private FinancialAiLegacyResponseMapper() {
    }

    public static FinancialAiResponse toUnified(IARequest request,
                                                IAResponse response,
                                                ApiVersion version,
                                                FinancialAiDescriptor descriptor) {
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(descriptor, "descriptor");

        Map<String, Object> outputs = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        String origin = "FINANCEIRA_" + version.name();
        FinancialAiStatus status = FinancialAiStatus.INDETERMINATE;
        double confidence = 0.0d;
        String message = null;
        Instant timestamp = Instant.now();

        if (response != null) {
            origin = response.getOrigem() != null ? response.getOrigem() : origin;
            status = FinancialAiStatus.from(response.getStatus());
            confidence = response.getConfianca() != null ? response.getConfianca() : 0.0d;
            message = response.getTexto();
            timestamp = response.getDataGeracao() != null ? response.getDataGeracao() : timestamp;
            mergeOutputs(outputs, response.getMetadados());
            mergeOutputs(outputs, response.getEssence());
            collectWarnings(warnings, response.getAlertasCriticos());
        }

        outputs.remove("financial_ai");
        outputs.remove("financial_ai_descriptor");

        return new FinancialAiResponse(
                request != null ? request.getRequestId() : null,
                request != null ? request.getCorrelationId() : null,
                request != null ? request.getAcao() : null,
                origin,
                version,
                status,
                confidence,
                message,
                timestamp,
                outputs,
                warnings,
                descriptor.capabilities()
        );
    }

    public static Map<String, Object> toEnvelope(FinancialAiResponse response) {
        Objects.requireNonNull(response, "response");
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("requestId", response.requestId());
        envelope.put("correlationId", response.correlationId());
        envelope.put("operation", response.operation());
        envelope.put("origin", response.origin());
        envelope.put("version", response.version().canonical());
        envelope.put("status", response.status().name());
        envelope.put("confidence", response.confidence());
        envelope.put("message", response.message());
        envelope.put("timestamp", response.timestamp());
        envelope.put("outputs", response.outputs());
        envelope.put("warnings", response.warnings());
        envelope.put("capabilities", response.capabilities());
        return Collections.unmodifiableMap(envelope);
    }

    public static Map<String, Object> toDescriptorEnvelope(FinancialAiDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("id", descriptor.id());
        envelope.put("version", descriptor.version().canonical());
        envelope.put("summary", descriptor.summary());
        envelope.put("capabilities", descriptor.capabilities());
        envelope.put("builtAt", descriptor.builtAt());
        return Collections.unmodifiableMap(envelope);
    }

    private static void mergeOutputs(Map<String, Object> outputs, Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        source.forEach((key, value) -> {
            if (key != null) {
                outputs.put(key, value);
            }
        });
    }

    private static void collectWarnings(List<String> warnings, List<String> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        Set<String> unique = new LinkedHashSet<>(warnings);
        for (String item : source) {
            if (item != null && !item.isBlank()) {
                unique.add(item);
            }
        }
        warnings.clear();
        warnings.addAll(unique);
    }
}
