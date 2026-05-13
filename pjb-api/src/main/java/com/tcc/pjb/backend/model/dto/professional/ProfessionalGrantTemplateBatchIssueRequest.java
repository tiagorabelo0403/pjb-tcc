package com.tcc.pjb.backend.model.dto.professional;

import java.util.List;

public record ProfessionalGrantTemplateBatchIssueRequest(
        String templateCode,
        String batchLabel,
        boolean autoApprove,
        List<ProfessionalGrantTemplateBatchItemRequest> items
) {
}
