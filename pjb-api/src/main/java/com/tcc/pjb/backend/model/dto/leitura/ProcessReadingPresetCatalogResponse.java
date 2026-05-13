package com.tcc.pjb.backend.model.dto.leitura;

import java.util.List;
import java.util.Map;

public record ProcessReadingPresetCatalogResponse(
        ProcessReadingPreferenceResponse activePreference,
        List<String> availableThemes,
        List<String> availableIntensities,
        List<String> availablePresets,
        List<String> availableFocusModes,
        List<String> availableOverlayModes,
        Map<String, Object> featureFlags,
        Map<String, Object> frontend
) {
}
