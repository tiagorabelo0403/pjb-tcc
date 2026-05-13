package com.tcc.pjb.backend.service.secretariat.query.routing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SecretariatConnectorDispatchProfile(
        String connectorId,
        String ackChannel,
        String replayDesk,
        String retryMode,
        String evidencePolicy,
        String dispatchWindow,
        List<String> labels,
        LinkedHashMap<String, Object> metadata) {

    public SecretariatConnectorDispatchProfile {
        labels = labels == null ? List.of() : List.copyOf(labels);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("connectorId", connectorId);
        out.put("ackChannel", ackChannel);
        out.put("replayDesk", replayDesk);
        out.put("retryMode", retryMode);
        out.put("evidencePolicy", evidencePolicy);
        out.put("dispatchWindow", dispatchWindow);
        out.put("labels", labels);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
