package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalResearchDossierResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationResponse;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class JuridicaVirtualTrendsCouncilService {

    public List<Map<String, Object>> resolveCouncil(ApiVersion version,
                                                    String capability,
                                                    String message,
                                                    LegalResearchDossierResponse dossier,
                                                    LegalValidationResponse validation,
                                                    LegalHallucinationGuardResponse guard,
                                                    Map<String, Object> context) {
        String normalizedMessage = message == null ? "" : message.trim();
        String capabilityToken = capability == null ? "LEGAL_CHAT" : capability.trim().toUpperCase(Locale.ROOT);
        String complexity = normalizedMessage.length() > 500 ? "ALTA" : normalizedMessage.length() > 180 ? "MEDIA" : "CONTROLADA";
        String versionLabel = version == null ? ApiVersion.latest().name() : version.name();
        String confidence = confidence(guard, validation);
        return JuridicaVirtualTrendsCatalog.profiles().stream()
                .map(trend -> lane(
                        trend.code(),
                        trend.role(),
                        complexity,
                        action(trend.action(), capabilityToken, versionLabel, context),
                        confidence
                ))
                .toList();
    }

    public String synthesizeHeadline(List<Map<String, Object>> council) {
        if (council == null || council.isEmpty()) {
            return "Conselho jurídico indisponível.";
        }
        return council.stream()
                .map(item -> String.valueOf(item.get("virtualTrend")))
                .reduce((left, right) -> left + " -> " + right)
                .orElse("Conselho jurídico indisponível.");
    }

    private Map<String, Object> lane(String trend,
                                     String role,
                                     String complexity,
                                     String action,
                                     String confidence) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("virtualTrend", trend);
        out.put("role", role);
        out.put("complexity", complexity);
        out.put("action", action);
        out.put("confidence", confidence);
        return ImmutableViewSupport.map(out);
    }

    private String action(String action, String capability, String version, Map<String, Object> context) {
        String processoId = context == null ? null : stringValue(context.get("processoId"));
        return processoId == null
                ? action + " em capability " + capability + " na versão " + version
                : action + " em capability " + capability + " na versão " + version + " para o processo " + processoId;
    }

    private String confidence(LegalHallucinationGuardResponse guard, LegalValidationResponse validation) {
        boolean grounded = guard != null && !"BLOCKED".equalsIgnoreCase(guard.status());
        boolean validated = validation != null && (validation.contradictions() == null || validation.contradictions().isEmpty());
        if (grounded && validated) {
            return "ALTA";
        }
        if (grounded) {
            return "CONTROLADA";
        }
        return "RESTRITA";
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }
}
