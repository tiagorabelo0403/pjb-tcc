package com.tcc.pjb.backend.service.processual.recursal.protocolo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record RecursalProtocolGovernanceProfile(
        String ackDesk,
        String receiptChannel,
        String retryMode,
        String evidencePolicy,
        String complianceDesk,
        String protocolWindow,
        List<String> labels,
        LinkedHashMap<String, Object> metadata) {

    public RecursalProtocolGovernanceProfile {
        labels = labels == null ? List.of() : List.copyOf(labels);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("ackDesk", ackDesk);
        out.put("receiptChannel", receiptChannel);
        out.put("retryMode", retryMode);
        out.put("evidencePolicy", evidencePolicy);
        out.put("complianceDesk", complianceDesk);
        out.put("protocolWindow", protocolWindow);
        out.put("labels", labels);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
