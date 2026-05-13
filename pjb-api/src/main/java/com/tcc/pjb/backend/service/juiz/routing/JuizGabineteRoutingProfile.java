package com.tcc.pjb.backend.service.juiz.routing;

import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import com.tcc.pjb.backend.service.secretariat.topology.NationalJudicialTopologyService.NationalJudicialTopologyProfile;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JuizGabineteRoutingProfile(
        String routeKey,
        String gabineteDesk,
        String gabineteInboxKey,
        String advisoryDesk,
        String hearingDesk,
        String coordinationDesk,
        String redistributionDesk,
        String sessionChannel,
        String organizationalPath,
        Duration captureSla,
        List<String> labels,
        Map<String, Object> metadata,
        NationalJudicialTopologyProfile topology,
        SecretariatOperationalRoutingProfile secretariatRouting) {

    public JuizGabineteRoutingProfile {
        labels = labels == null ? List.of() : List.copyOf(labels);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
    }

    public String captureTemplateCode(Long processoId) {
        return "JUIZ:GABINETE:CAPTURA:" + routeKey + ':' + processoId;
    }

    public String releaseTemplateCode(String stage, Long processoId) {
        String normalizedStage = stage == null || stage.isBlank() ? "EXECUCAO" : stage.trim().toUpperCase();
        return "JUIZ:GABINETE:LIBERACAO:" + normalizedStage + ':' + routeKey + ':' + processoId;
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("routeKey", routeKey);
        out.put("gabineteDesk", gabineteDesk);
        out.put("gabineteInboxKey", gabineteInboxKey);
        out.put("advisoryDesk", advisoryDesk);
        out.put("hearingDesk", hearingDesk);
        out.put("coordinationDesk", coordinationDesk);
        out.put("redistributionDesk", redistributionDesk);
        out.put("sessionChannel", sessionChannel);
        out.put("organizationalPath", organizationalPath);
        out.put("captureSlaHours", captureSla == null ? null : captureSla.toHours());
        out.put("labels", labels);
        out.put("metadata", metadata);
        out.put("topology", topology == null ? null : topology.toMap());
        out.put("secretariatRouting", secretariatRouting == null ? null : secretariatRouting.toMap());
        out.entrySet().removeIf(entry -> entry.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
