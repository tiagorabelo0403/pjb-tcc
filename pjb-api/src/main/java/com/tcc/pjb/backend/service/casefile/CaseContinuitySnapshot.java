package com.tcc.pjb.backend.service.casefile;

import java.util.List;
import java.util.Objects;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;

public record CaseContinuitySnapshot(
        Long caseFileId,
        Long rootProcessoId,
        Long requestedProcessoId,
        String anchorProceedingKey,
        CaseContinuityTrack dominantTrack,
        List<CaseContinuityProceedingNode> proceedings,
        List<CaseContinuityEdgeLink> edges,
        List<String> warnings
) {
    public CaseContinuitySnapshot {
        proceedings = proceedings == null ? List.of() : List.copyOf(proceedings);
        edges = edges == null ? List.of() : List.copyOf(edges);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public boolean isUnifiedRoot() {
        return caseFileId != null && rootProcessoId != null && anchorProceedingKey != null;
    }

    public int proceedingCount() {
        return proceedings.size();
    }

    public boolean hasRecursalBranch() {
        return proceedings.stream().anyMatch(CaseContinuityProceedingNode::isRecursalBranch);
    }

    public boolean hasExecutoryBranch() {
        return proceedings.stream().anyMatch(CaseContinuityProceedingNode::isExecutoryBranch);
    }

    public boolean hasArchivedBranch() {
        return proceedings.stream().anyMatch(CaseContinuityProceedingNode::isArchivedState);
    }

    public boolean hasStructuredContinuation() {
        return !edges.isEmpty();
    }

    public boolean hasShadowProceedings() {
        return proceedings.stream().anyMatch(CaseContinuityProceedingNode::shadow);
    }

    public boolean isRootRequestedProcesso() {
        return Objects.equals(rootProcessoId, requestedProcessoId);
    }

    public boolean requiresAttention() {
        return !warnings.isEmpty();
    }
}
