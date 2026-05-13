package com.tcc.pjb.backend.model.dto.institutional;

import java.time.Instant;
import java.util.List;

public record InstitutionalWorkbenchWorkspaceResponse(
        Instant generatedAt,
        InstitutionalWorkbenchProfileResponse profile,
        List<InstitutionalWorkbenchMetricResponse> headlineMetrics,
        List<InstitutionalWorkbenchWidgetResponse> widgets,
        List<InstitutionalWorkbenchRouteResponse> routes,
        InstitutionalWorkbenchQuickActionsResponse quickActions,
        InstitutionalWorkbenchOperationalQueueResponse operationalQueue,
        List<String> warnings
) {
    public InstitutionalWorkbenchWorkspaceResponse {
        headlineMetrics = headlineMetrics == null ? List.of() : List.copyOf(headlineMetrics);
        widgets = widgets == null ? List.of() : List.copyOf(widgets);
        routes = routes == null ? List.of() : List.copyOf(routes);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
