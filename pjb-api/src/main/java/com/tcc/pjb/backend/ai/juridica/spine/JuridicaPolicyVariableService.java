package com.tcc.pjb.backend.ai.juridica.spine;

import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class JuridicaPolicyVariableService {

    public Map<String, Object> resolve(ApiVersion version, String capability, Map<String, Object> payload) {
        ApiVersion effectiveVersion = version == null ? ApiVersion.latest() : version;
        String normalizedCapability = capability == null ? "LEGAL_GENERAL_ASSIST_V3" : capability.toUpperCase(Locale.ROOT);
        boolean sigilo = payload != null && Boolean.TRUE.equals(payload.get("sigilo"));
        boolean petitionDetected = payload != null && (Boolean.TRUE.equals(payload.get("peticaoDetectada")) || payload.containsKey("textoPeticaoLivre"));
        String branch = value(payload, "ramoDireito", "ramo", "materia");
        String rite = value(payload, "ritoProcessual", "rito", "procedimento");

        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("branch", branch == null ? "NAO_INFORMADO" : branch);
        out.put("rite", rite == null ? "NAO_INFORMADO" : rite);
        out.put("sigilo", sigilo);
        out.put("citationFirst", effectiveVersion.isAtLeast(ApiVersion.V3));
        out.put("authorityFloor", effectiveVersion.isAtLeast(ApiVersion.V3) ? "PRECEDENTE_QUALIFICADO_OU_NORMA_CANONICA" : "NORMA_CANONICA_OU_CURRICULUM");
        out.put("proceduralValidation", effectiveVersion.isAtLeast(ApiVersion.V2));
        out.put("symbolicValidation", true);
        out.put("petitionDetected", petitionDetected);
        out.put("capabilityFamily", normalizedCapability);
        return Collections.unmodifiableMap(out);
    }

    private static String value(Map<String, Object> payload, String... keys) {
        if (payload == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = payload.get(key);
            if (value instanceof String text && !text.isBlank()) {
                return text.trim();
            }
        }
        return null;
    }
}
