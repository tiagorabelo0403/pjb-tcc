package com.tcc.pjb.backend.model.dto.leitura;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record ProcessReadingPresetCatalogResponse(
        ProcessReadingPreferenceResponse activePreference,
        List<String> availableThemes,
        List<String> availableIntensities,
        List<String> availablePresets,
        List<String> availableFocusModes,
        List<String> availableOverlayModes,
        @Schema(description = "Feature flags do preset de leitura — chaves conhecidas: supportsInstitutionalPreset, supportsAmberMode, supportsPrivacyVeil, supportsKeyboardBias, supportsChronology, supportsCitationMap, supportsOperationalOverlay, supportsNativeActs, supportsInlineDecisions, supportsProceduralContextMesh, supportsReadingSpecialization")
        @Size(max = 20)
        Map<String, Boolean> featureFlags,
        @Schema(description = "Endpoints e opções de configuração do catálogo de presets — URLs de navegação e listas de opções de preset")
        @Size(max = 20)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> frontend
) {
}
