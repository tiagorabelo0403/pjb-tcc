package com.tcc.pjb.backend.model.dto.institutional.support.panel;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record InstitutionalSupportPanelSnapshotResponse(
        Instant generatedAt,
        Map<String, Object> lane,
        Map<String, Object> metrics,
        List<InstitutionalSupportPanelItemResponse> items,
        List<InstitutionalSupportPanelGroupResponse> byProcesso,
        List<InstitutionalSupportPanelGroupResponse> byRito,
        List<InstitutionalSupportPanelGroupResponse> byData,
        Map<String, Object> credential,
        Map<String, Object> routes,
        List<String> warnings
) {
}
