package com.tcc.pjb.backend.model.dto.professional;

import java.util.List;

public record ProfessionalGrantBatchDecisionRequest(
        List<Long> grantIds,
        String reason
) {
}
