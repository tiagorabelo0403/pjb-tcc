package com.tcc.pjb.backend.model.dto.magistratura;

import java.util.List;
import java.util.Map;

public record MagistraturaJudicialActPreviewResponse(
        Long processoId,
        String processoNumero,
        MagistraturaJudicialActCode action,
        boolean allowed,
        String verdict,
        String lane,
        String suggestedTitle,
        String nativeRoute,
        String template,
        List<String> reasons,
        List<String> warnings,
        List<MagistraturaJudicialActFieldResponse> fields,
        List<MagistraturaJudicialProvidenceResponse> providences,
        Map<String, Object> metrics
) {
}
