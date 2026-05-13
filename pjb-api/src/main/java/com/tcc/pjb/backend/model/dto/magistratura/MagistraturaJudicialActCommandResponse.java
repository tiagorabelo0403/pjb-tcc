package com.tcc.pjb.backend.model.dto.magistratura;

import java.util.List;
import java.util.Map;

public record MagistraturaJudicialActCommandResponse(
        MagistraturaJudicialActCode action,
        String lane,
        String status,
        Long processoId,
        List<String> reasons,
        List<MagistraturaJudicialProvidenceDispatchResponse> providences,
        Map<String, Object> payload
) {
}
