package com.tcc.pjb.backend.service.ui.accessibility.engine;

import java.util.List;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityFlag;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityPreset;

public record AccessibilityEvaluation(
    UiAccessibilityPreset legacyPreset,
    long flagsMask,
    List<UiAccessibilityFlag> flags,
    int score,
    double probability,
    double confidence,
    List<String> reasonCodes,
    List<String> reasons,
    String suggestionHash
) {


    public UiAccessibilityPreset preset() {
        return legacyPreset;
    }
}
