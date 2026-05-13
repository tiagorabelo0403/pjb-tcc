package com.tcc.pjb.backend.service.institutional.workbench;

import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchActionResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchMetricResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchOperationalQueueResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchProfileResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchQuickActionsResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchRouteResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchWidgetResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalWorkbenchWidgetPolicyEngine {

    public Projection project(InstitutionalWorkbenchProfileResponse profile,
                              InstitutionalWorkbenchQuickActionsResponse quickActions,
                              InstitutionalWorkbenchOperationalQueueResponse operationalQueue) {
        List<InstitutionalWorkbenchMetricResponse> metrics = headlineMetrics(profile, quickActions, operationalQueue);
        List<InstitutionalWorkbenchWidgetResponse> widgets = widgets(profile, quickActions, operationalQueue);
        List<InstitutionalWorkbenchRouteResponse> routes = routes(quickActions, operationalQueue);
        return new Projection(metrics, widgets, routes);
    }

    private List<InstitutionalWorkbenchMetricResponse> headlineMetrics(InstitutionalWorkbenchProfileResponse profile,
                                                                       InstitutionalWorkbenchQuickActionsResponse quickActions,
                                                                       InstitutionalWorkbenchOperationalQueueResponse operationalQueue) {
        int enabledActions = (int) quickActions.actions().stream().filter(InstitutionalWorkbenchActionResponse::enabled).count();
        int blockedActions = quickActions.actions().size() - enabledActions;
        return List.of(
                new InstitutionalWorkbenchMetricResponse("scope", "Escopo", profile.federativeSphere(), "STABLE", "INFO"),
                new InstitutionalWorkbenchMetricResponse("queue_total", "Fila operacional", String.valueOf(operationalQueue.totalItems()), "STABLE", operationalQueue.totalItems() == 0 ? "WARNING" : "INFO"),
                new InstitutionalWorkbenchMetricResponse("queue_actionable", "Itens acionáveis", String.valueOf(operationalQueue.actionableItems()), operationalQueue.actionableItems() > 0 ? "UP" : "STABLE", operationalQueue.actionableItems() > 0 ? "SUCCESS" : "WARNING"),
                new InstitutionalWorkbenchMetricResponse("actions_enabled", "Quick actions liberadas", String.valueOf(enabledActions), enabledActions > 0 ? "UP" : "STABLE", enabledActions > 0 ? "SUCCESS" : "WARNING"),
                new InstitutionalWorkbenchMetricResponse("actions_blocked", "Quick actions bloqueadas", String.valueOf(blockedActions), blockedActions > 0 ? "UP" : "STABLE", blockedActions > 0 ? "WARNING" : "INFO")
        );
    }

    private List<InstitutionalWorkbenchWidgetResponse> widgets(InstitutionalWorkbenchProfileResponse profile,
                                                               InstitutionalWorkbenchQuickActionsResponse quickActions,
                                                               InstitutionalWorkbenchOperationalQueueResponse operationalQueue) {
        ArrayList<InstitutionalWorkbenchWidgetResponse> widgets = new ArrayList<>();
        widgets.add(new InstitutionalWorkbenchWidgetResponse(
                "institutional_scope",
                "Escopo institucional",
                "PROFILE",
                true,
                10,
                "/api/v1/institucional/workbench",
                profile.headline(),
                Map.of(
                        "branch", profile.institutionalBranch(),
                        "materialFocus", profile.materialFocus(),
                        "justiceMesh", profile.justiceMesh(),
                        "territorialAnchors", profile.territorialAnchors(),
                        "capabilities", profile.capabilities()
                ),
                List.of()
        ));
        widgets.add(new InstitutionalWorkbenchWidgetResponse(
                "quick_actions",
                "Ações rápidas do gabinete",
                "ACTIONS",
                true,
                20,
                "/api/v1/institucional/workbench/quick-actions",
                summaryQuickActions(quickActions),
                payload(
                        "total", quickActions.actions().size(),
                        "enabled", quickActions.actions().stream().filter(InstitutionalWorkbenchActionResponse::enabled).count(),
                        "firstEnabledRoute", quickActions.actions().stream().filter(InstitutionalWorkbenchActionResponse::enabled).findFirst().map(InstitutionalWorkbenchActionResponse::route).orElse(null)
                ),
                quickActions.warnings()
        ));
        widgets.add(new InstitutionalWorkbenchWidgetResponse(
                "operational_queue",
                "Fila operacional",
                "QUEUE",
                true,
                30,
                "/api/v1/institucional/workbench/operational-queue",
                summaryQueue(operationalQueue),
                payload(
                        "totalItems", operationalQueue.totalItems(),
                        "actionableItems", operationalQueue.actionableItems(),
                        "blockedItems", operationalQueue.blockedItems()
                ),
                operationalQueue.warnings()
        ));
        widgets.add(new InstitutionalWorkbenchWidgetResponse(
                "material_guard",
                "Atuação material governada",
                "GUARD",
                true,
                40,
                "/api/v1/institucional/material-guard/processos/{processoId}",
                "Mesma regra central aplicada em botões, filas e protocolo institucional.",
                payload(
                        "supportsExplainability", true,
                        "supportsRedirect", true,
                        "profileBranch", profile.institutionalBranch()
                ),
                List.of()
        ));
        return List.copyOf(widgets);
    }

    private List<InstitutionalWorkbenchRouteResponse> routes(InstitutionalWorkbenchQuickActionsResponse quickActions,
                                                             InstitutionalWorkbenchOperationalQueueResponse operationalQueue) {
        Set<String> seen = new LinkedHashSet<>();
        ArrayList<InstitutionalWorkbenchRouteResponse> routes = new ArrayList<>();
        routes.add(route(seen, "workbench", "Workbench institucional", "/api/v1/institucional/workbench", "GET", true));
        routes.add(route(seen, "quick_actions", "Quick actions", "/api/v1/institucional/workbench/quick-actions", "GET", false));
        routes.add(route(seen, "operational_queue", "Fila operacional", "/api/v1/institucional/workbench/operational-queue", "GET", false));
        quickActions.actions().stream()
                .filter(InstitutionalWorkbenchActionResponse::enabled)
                .limit(4)
                .forEach(action -> routes.add(route(seen, action.code().toLowerCase(), action.label(), action.route(), action.method(), false)));
        operationalQueue.items().stream()
                .map(item -> item.primaryAction())
                .filter(action -> action != null && action.enabled())
                .limit(3)
                .forEach(action -> routes.add(route(seen, (action.code() + "_queue").toLowerCase(), action.label(), action.route(), action.method(), false)));
        return routes.stream().filter(java.util.Objects::nonNull).toList();
    }

    private InstitutionalWorkbenchRouteResponse route(Set<String> seen,
                                                      String code,
                                                      String label,
                                                      String route,
                                                      String method,
                                                      boolean primary) {
        if (route == null || route.isBlank()) {
            return null;
        }
        if (!seen.add(method + ":" + route)) {
            return null;
        }
        return new InstitutionalWorkbenchRouteResponse(code, label, route, method, primary);
    }

    private String summaryQuickActions(InstitutionalWorkbenchQuickActionsResponse quickActions) {
        long enabled = quickActions.actions().stream().filter(InstitutionalWorkbenchActionResponse::enabled).count();
        return enabled + " de " + quickActions.actions().size() + " quick actions estão liberadas para a malha atual.";
    }

    private String summaryQueue(InstitutionalWorkbenchOperationalQueueResponse operationalQueue) {
        return operationalQueue.actionableItems() + " itens acionáveis e " + operationalQueue.blockedItems() + " itens com restrição material.";
    }


    private Map<String, Object> payload(Object... values) {
        java.util.LinkedHashMap<String, Object> payload = new java.util.LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            Object key = values[i];
            if (key instanceof String name && !name.isBlank()) {
                payload.put(name, values[i + 1]);
            }
        }
        return payload;
    }

    public record Projection(
            List<InstitutionalWorkbenchMetricResponse> metrics,
            List<InstitutionalWorkbenchWidgetResponse> widgets,
            List<InstitutionalWorkbenchRouteResponse> routes
    ) {
        public Projection {
            metrics = metrics == null ? List.of() : List.copyOf(metrics);
            widgets = widgets == null ? List.of() : List.copyOf(widgets);
            routes = routes == null ? List.of() : List.copyOf(routes);
        }
    }
}
