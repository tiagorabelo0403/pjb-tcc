package com.tcc.pjb.backend.model.dto.institutional.support.operations;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record InstitutionalSupportPrepautaSnapshotResponse(
        Instant generatedAt,
        Map<String, Object> lane,
        Map<String, Object> processo,
        Map<String, Object> metrics,
        List<TimelineItem> timeline,
        List<ChecklistItem> checklist,
        List<PendingAct> pendingActs,
        List<DocumentTemplate> projectedDocuments,
        Map<String, Object> contactEnvelope,
        List<String> warnings,
        Map<String, Object> routes
) {
    public record TimelineItem(
            Long workItemId,
            String title,
            String status,
            String queueCode,
            Instant referenceAt,
            boolean blocking,
            List<String> tags
    ) {
    }

    public record ChecklistItem(
            String code,
            String label,
            String status,
            String severity,
            String detail
    ) {
    }

    public record PendingAct(
            String actCode,
            String actLabel,
            String severity,
            Instant dueAt,
            boolean blocking,
            List<String> signals
    ) {
    }

    public record DocumentTemplate(
            String documentCode,
            String title,
            String actAxis,
            String targetPhase,
            boolean sensitive,
            List<String> tags
    ) {
    }
}
