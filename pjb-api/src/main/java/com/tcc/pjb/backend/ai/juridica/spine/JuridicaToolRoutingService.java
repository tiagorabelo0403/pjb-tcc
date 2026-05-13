package com.tcc.pjb.backend.ai.juridica.spine;

import com.tcc.pjb.backend.ai.juridica.mesh.JuridicaLegalToolCatalogService;
import com.tcc.pjb.backend.model.dto.ai.legal.mesh.LegalAiToolDescriptor;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JuridicaToolRoutingService {

    private final JuridicaLegalToolCatalogService toolCatalogService;

    public JuridicaToolRoutingService(JuridicaLegalToolCatalogService toolCatalogService) {
        this.toolCatalogService = Objects.requireNonNull(toolCatalogService, "toolCatalogService");
    }

    public List<LegalAiToolDescriptor> resolve(ApiVersion version, String capability, Map<String, Object> payload) {
        String normalizedCapability = capability == null ? "LEGAL_GENERAL_ASSIST_V3" : capability.toUpperCase(Locale.ROOT);
        boolean strict = payload != null && (Boolean.TRUE.equals(payload.get("sigilo")) || normalizedCapability.contains("PARECER") || normalizedCapability.contains("PROTOCOLO"));
        boolean petitionDetected = payload != null && (Boolean.TRUE.equals(payload.get("peticaoDetectada")) || payload.containsKey("textoPeticaoLivre"));
        List<LegalAiToolDescriptor> tools = toolCatalogService.resolve(normalizedCapability, version, strict, petitionDetected);
        if (strict) {
            return tools.stream().filter(tool -> tool.readOnly() || !tool.requiresStepUp()).toList();
        }
        return tools;
    }
}
