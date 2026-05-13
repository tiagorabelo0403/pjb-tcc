package com.tcc.pjb.backend.core.distribuicao;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DistributionConstraintSnapshotService {

    public DistributionConstraintSnapshot resolve(Map<String, Object> routingMetadata,
                                                  String numeroProcesso,
                                                  String preventionReference,
                                                  String processoReferencia) {
        String relationMode = metadataString(routingMetadata, "relational.binding.relationMode");
        String attachmentMode = metadataString(routingMetadata, "relational.attachmentMode");
        String registryBucket = metadataString(routingMetadata, "relational.registryBucket");
        String preventionFingerprint = metadataString(routingMetadata, "relational.binding.preventionFingerprint");
        String dependencyFingerprint = metadataString(routingMetadata, "relational.binding.dependencyFingerprint");
        String bindingStrength = metadataString(routingMetadata, "relational.binding.bindingStrength");
        boolean reviewRequired = Boolean.parseBoolean(firstNonBlank(metadataString(routingMetadata, "relational.binding.strictLock"), "false"))
                || metadataString(routingMetadata, "relational.binding.triageOverride") != null;
        String queueSuffix = firstNonBlank(registryBucket, normalize(preventionReference), normalize(processoReferencia), normalize(numeroProcesso), "BASE");
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("triageOverride", metadataString(routingMetadata, "relational.binding.triageOverride"));
        metadata.put("strictLock", metadataString(routingMetadata, "relational.binding.strictLock"));
        metadata.put("autoAttachAllowed", metadataString(routingMetadata, "relational.binding.autoAttachAllowed"));
        return new DistributionConstraintSnapshot(
                relationMode,
                attachmentMode,
                registryBucket,
                preventionFingerprint,
                dependencyFingerprint,
                bindingStrength,
                queueSuffix,
                reviewRequired,
                metadata
        );
    }

    private String metadataString(Map<String, Object> metadata, String dottedPath) {
        if (metadata == null || dottedPath == null || dottedPath.isBlank()) {
            return null;
        }
        Object current = metadata;
        for (String token : dottedPath.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(token);
            if (current == null) {
                return null;
            }
        }
        if (current instanceof String value) {
            return value.isBlank() ? null : value.trim();
        }
        return String.valueOf(current);
    }

    private String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("(^_|_$)", "");
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
