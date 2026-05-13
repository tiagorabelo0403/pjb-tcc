package com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record InstitutionalPanelProvisioningReadiness(
        String profileCode,
        String panelCode,
        String initialRoute,
        boolean blueprintMatched,
        boolean workspaceBound,
        boolean routeReady,
        boolean sectionsReady,
        boolean quickActionsReady,
        boolean guardsReady,
        boolean visibilityRulesReady,
        boolean tabsReady,
        boolean workspaceActionsReady,
        boolean authorityBandsReady,
        boolean separatorsReady,
        boolean notificationsReady,
        boolean calendarReady,
        boolean hearingsReady,
        boolean readingModeReady,
        boolean triageReady,
        boolean presentationReady,
        boolean colorSystemReady,
        boolean opinionFlowReady,
        boolean calculatorReady,
        boolean sharedExperienceReady,
        boolean complete,
        int totalBlueprints,
        int totalPrimarySections,
        int totalQuickActions,
        int totalSecurityGuards,
        int totalVisibilityRules,
        int totalTabs,
        int totalWorkspaceActions,
        int totalAuthorityBands,
        int totalSeparators,
        int totalSharedExperienceSurfaces,
        int totalSharedExperienceSurfacesReady,
        List<String> primarySections,
        List<String> quickActions,
        List<String> securityGuards,
        List<String> visibilityRules,
        List<String> tabs,
        List<String> readySharedExperienceSurfaces,
        List<String> missingSharedExperienceSurfaces,
        List<String> findings,
        List<String> fundamentos,
        InstitutionalHearingSchedulingGovernance hearingGovernance,
        InstitutionalOperationalDeskGovernance deskGovernance,
        Instant generatedAt
) {
    public InstitutionalPanelProvisioningReadiness {
        Objects.requireNonNull(primarySections);
        Objects.requireNonNull(quickActions);
        Objects.requireNonNull(securityGuards);
        Objects.requireNonNull(visibilityRules);
        Objects.requireNonNull(tabs);
        Objects.requireNonNull(readySharedExperienceSurfaces);
        Objects.requireNonNull(missingSharedExperienceSurfaces);
        Objects.requireNonNull(findings);
        Objects.requireNonNull(fundamentos);
        Objects.requireNonNull(hearingGovernance);
        Objects.requireNonNull(deskGovernance);
        Objects.requireNonNull(generatedAt);
    }
}
