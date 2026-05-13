package com.tcc.pjb.backend.model.dto.institutional.support.panel;

import java.util.List;

public record InstitutionalSupportPanelGroupResponse(
        String key,
        String label,
        long count,
        List<InstitutionalSupportPanelItemResponse> items
) {
}
