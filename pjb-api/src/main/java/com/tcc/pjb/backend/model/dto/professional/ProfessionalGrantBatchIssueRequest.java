package com.tcc.pjb.backend.model.dto.professional;

import java.util.List;

public record ProfessionalGrantBatchIssueRequest(
        String batchLabel,
        boolean autoApprove,
        List<ProfessionalGrantBatchItemRequest> items
) {
}
