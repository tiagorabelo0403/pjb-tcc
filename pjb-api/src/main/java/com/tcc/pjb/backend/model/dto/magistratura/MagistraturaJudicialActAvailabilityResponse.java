package com.tcc.pjb.backend.model.dto.magistratura;

import java.util.List;

public record MagistraturaJudicialActAvailabilityResponse(
        MagistraturaJudicialActCode code,
        String label,
        String lane,
        boolean enabled,
        String verdict,
        String nativeRoute,
        String template,
        List<String> reasons,
        List<String> warnings,
        List<MagistraturaJudicialActFieldResponse> fields
) {
}
