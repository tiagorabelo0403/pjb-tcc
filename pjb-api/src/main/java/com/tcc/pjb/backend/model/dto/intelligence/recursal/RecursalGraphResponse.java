package com.tcc.pjb.backend.model.dto.intelligence.recursal;

import java.util.List;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;

public record RecursalGraphResponse(
        Long caseFileId,
        String anchorProceedingKey,
        SummaryDto summary,
        List<NodeDto> nodes,
        List<EdgeDto> edges
) {

    public RecursalGraphResponse {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        edges = edges == null ? List.of() : List.copyOf(edges);
    }

    public record SummaryDto(
            int totalNodes,
            int totalEdges,
            int predictedNodes,
            int activeNodes,
            int reconciledNodes,
            InstanceLevel maxInstance
    ) {}

    public record NodeDto(
            String proceedingKey,
            boolean shadow,
            String status,
            InstanceLevel instanceLevel,
            String court,
            String numeroUnificado,
            Long linkedProcessoId,
            String secrecy,
            String sourceSystem,
            String displayLabel
    ) {}

    public record EdgeDto(
            String fromProceedingKey,
            String toProceedingKey,
            String relationType,
            String appealType
    ) {}
}
