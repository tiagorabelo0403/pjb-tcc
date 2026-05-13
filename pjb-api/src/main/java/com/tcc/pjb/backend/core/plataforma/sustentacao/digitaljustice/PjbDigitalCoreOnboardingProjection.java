package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import java.util.List;

public record PjbDigitalCoreOnboardingProjection(
        String title,
        String targetInstitution,
        List<String> steps,
        List<String> configurationChecklist,
        List<String> safetyGuards
) {
}
