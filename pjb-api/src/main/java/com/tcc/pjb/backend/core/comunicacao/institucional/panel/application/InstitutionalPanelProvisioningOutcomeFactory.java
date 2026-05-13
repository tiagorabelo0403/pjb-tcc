package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalHearingSchedulingGovernance;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalOperationalDeskGovernance;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelProvisioningReadiness;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;

final class InstitutionalPanelProvisioningOutcomeFactory {

    private static final String CALENDAR_SURFACE_ROUTE = "/api/v1/calendar/events";
    private static final String HEARING_SURFACE_ROUTE = "/api/v1/processual/pauta-audiencia";
    private static final String READING_SURFACE_ROUTE = "/api/v1/processos/{processoId}/painel-leitura";
    private static final String TRIAGE_SURFACE_ROUTE = "/api/v1/intelligence/triagem";
    private static final String PRESENTATION_SURFACE_ROUTE = "/api/v1/ui/presentation";
    private static final String ACCESSIBILITY_SURFACE_ROUTE = "/api/v1/ui/accessibility";
    private static final String CALCULATOR_SURFACE_ROUTE = "/api/v1/processual/calculos/workspace";

    private final InstitutionalPanelProvisioningSupport support;

    InstitutionalPanelProvisioningOutcomeFactory(InstitutionalPanelProvisioningSupport support) {
        this.support = support;
    }

    InstitutionalPanelProvisioningReadiness create(InstitutionalOperationalProfileProjection profile,
                                                   InstitutionalPanelProvisioningContext context,
                                                   InstitutionalPanelProvisioningSnapshot snapshot,
                                                   Instant now) {
        InstitutionalProcessWorkspace workspace = context.workspace();
        InstitutionalHearingSchedulingGovernance hearingGovernance = context.hearingGovernance();
        InstitutionalOperationalDeskGovernance deskGovernance = context.deskGovernance();

        boolean blueprintMatched = !context.blueprints().isEmpty();
        boolean workspaceBound = workspace != null;
        boolean routeReady = !support.isBlank(support.firstNonBlank(profile.landingPath(), snapshot.initialRoute()));
        boolean sectionsReady = !snapshot.primarySections().isEmpty();
        boolean quickActionsReady = !snapshot.quickActions().isEmpty();
        boolean guardsReady = !snapshot.securityGuards().isEmpty();
        boolean visibilityRulesReady = !snapshot.visibilityRules().isEmpty();
        boolean tabsReady = !snapshot.tabs().isEmpty();
        boolean workspaceActionsReady = workspace != null && !workspace.actions().isEmpty();
        boolean authorityBandsReady = workspace != null && !workspace.authorityBands().isEmpty();
        boolean separatorsReady = workspace != null && !workspace.separators().isEmpty();

        boolean notificationsReady = blueprintMatched && workspaceBound && !support.isBlank(InstitutionalApiRoutes.notificacoes());
        boolean calendarReady = blueprintMatched && workspaceBound && !support.isBlank(CALENDAR_SURFACE_ROUTE);
        boolean hearingsReady = workspaceBound
                && hearingGovernance.sectionVisible()
                && !support.isBlank(HEARING_SURFACE_ROUTE)
                && (calendarReady || support.containsDomainSignals(snapshot.primarySections(), snapshot.quickActions(), snapshot.tabs(), "AUDIENCIA", "PAUTA", "CONCILIACAO", "CUSTODIA"));
        boolean readingModeReady = workspaceBound && !support.isBlank(READING_SURFACE_ROUTE) && !support.isBlank(profile.processProfile());
        boolean triageReady = blueprintMatched && !support.isBlank(TRIAGE_SURFACE_ROUTE) && (sectionsReady || quickActionsReady);
        boolean presentationReady = !support.isBlank(PRESENTATION_SURFACE_ROUTE) && !support.isBlank(ACCESSIBILITY_SURFACE_ROUTE);
        boolean colorSystemReady = presentationReady && !support.isBlank(profile.accentColor()) && (workspace == null || !support.isBlank(workspace.accentColor()));
        boolean opinionRelevant = support.requiresOpinionFlow(profile, context.catalogEntry(), workspace);
        boolean opinionFlowReady = !opinionRelevant || support.containsDomainSignals(snapshot.primarySections(), snapshot.quickActions(), snapshot.tabs(), "PARECER", "DEFESA", "MANIFESTACAO", "MINUTA", "INFORMACOES");
        boolean calculatorRelevant = support.requiresCalculator(profile, context.catalogEntry(), workspace);
        boolean calculatorReady = !calculatorRelevant || blueprintMatched && workspaceBound && !support.isBlank(CALCULATOR_SURFACE_ROUTE);
        boolean sharedExperienceReady = notificationsReady
                && calendarReady
                && hearingsReady
                && readingModeReady
                && triageReady
                && presentationReady
                && colorSystemReady
                && calculatorReady;
        boolean deskGovernanceReady = !deskGovernance.sectionVisible()
                || deskGovernance.unitScopeBound()
                && deskGovernance.segregatedByUnit()
                && !deskGovernance.deskQueues().isEmpty()
                && !deskGovernance.assignmentBoundaries().isEmpty();
        boolean structuralComplete = blueprintMatched
                && workspaceBound
                && routeReady
                && sectionsReady
                && quickActionsReady
                && guardsReady
                && visibilityRulesReady
                && tabsReady
                && workspaceActionsReady
                && authorityBandsReady
                && separatorsReady;
        boolean complete = structuralComplete
                && sharedExperienceReady
                && opinionFlowReady
                && calculatorReady
                && deskGovernanceReady
                && (!hearingGovernance.sectionVisible() || hearingGovernance.canRequestHearing());

        LinkedHashSet<String> readySharedSurfaces = new LinkedHashSet<>();
        LinkedHashSet<String> missingSharedSurfaces = new LinkedHashSet<>();
        LinkedHashSet<String> findings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(snapshot.fundamentos());

        registerSharedSurface(notificationsReady, readySharedSurfaces, missingSharedSurfaces, "NOTIFICACOES", InstitutionalApiRoutes.notificacoes(), fundamentos, findings, InstitutionalPanelProvisioningMessages.FINDING_NOTIFICATIONS);
        registerSharedSurface(calendarReady, readySharedSurfaces, missingSharedSurfaces, "CALENDARIO_UNIFICADO", CALENDAR_SURFACE_ROUTE, fundamentos, findings, InstitutionalPanelProvisioningMessages.FINDING_CALENDAR);
        registerSharedSurface(hearingsReady, readySharedSurfaces, missingSharedSurfaces, "DATAS_AUDIENCIA", HEARING_SURFACE_ROUTE, fundamentos, findings, InstitutionalPanelProvisioningMessages.FINDING_HEARINGS);
        registerSharedSurface(readingModeReady, readySharedSurfaces, missingSharedSurfaces, "MODO_LEITURA", READING_SURFACE_ROUTE, fundamentos, findings, InstitutionalPanelProvisioningMessages.FINDING_READING);
        registerSharedSurface(triageReady, readySharedSurfaces, missingSharedSurfaces, "TRIAGEM", TRIAGE_SURFACE_ROUTE, fundamentos, findings, InstitutionalPanelProvisioningMessages.FINDING_TRIAGE);
        registerSharedSurface(presentationReady, readySharedSurfaces, missingSharedSurfaces, "APRESENTACAO", PRESENTATION_SURFACE_ROUTE, fundamentos, findings, InstitutionalPanelProvisioningMessages.FINDING_PRESENTATION);
        registerSharedSurface(colorSystemReady, readySharedSurfaces, missingSharedSurfaces, "CORES", ACCESSIBILITY_SURFACE_ROUTE, fundamentos, findings, InstitutionalPanelProvisioningMessages.FINDING_COLORS);
        if (opinionFlowReady) {
            fundamentos.add(InstitutionalPanelProvisioningMessages.OPINION_READY);
        } else if (opinionRelevant) {
            findings.add(InstitutionalPanelProvisioningMessages.FINDING_OPINION);
        }
        if (calculatorReady) {
            fundamentos.add(InstitutionalPanelProvisioningMessages.CALCULATOR_READY);
        } else if (calculatorRelevant) {
            findings.add(InstitutionalPanelProvisioningMessages.FINDING_CALCULATOR);
        }
        if (hearingGovernance.sectionVisible() && (hearingGovernance.findings().isEmpty() || hearingGovernance.canRequestHearing())) {
            fundamentos.add(InstitutionalPanelProvisioningMessages.HEARING_GOVERNANCE_READY);
        } else if (hearingGovernance.sectionVisible()) {
            findings.add(InstitutionalPanelProvisioningMessages.FINDING_HEARING_GOVERNANCE);
            findings.addAll(hearingGovernance.findings());
        }
        fundamentos.addAll(hearingGovernance.fundamentos());
        if (deskGovernanceReady) {
            fundamentos.add(InstitutionalPanelProvisioningMessages.DESK_GOVERNANCE_READY);
        }
        if (deskGovernance.sectionVisible() && !deskGovernanceReady && !deskGovernance.findings().isEmpty()) {
            findings.add(InstitutionalPanelProvisioningMessages.FINDING_DESK_GOVERNANCE);
            findings.addAll(deskGovernance.findings());
        }
        fundamentos.addAll(deskGovernance.fundamentos());

        fundamentos.add(InstitutionalPanelProvisioningMessages.panel(profile.panelCode()));
        fundamentos.add(InstitutionalPanelProvisioningMessages.profileKey(support.nullSafe(profile.profileKey())));
        fundamentos.add(InstitutionalPanelProvisioningMessages.processProfile(support.nullSafe(profile.processProfile())));
        fundamentos.add(InstitutionalPanelProvisioningMessages.nominationRole(support.nullSafe(profile.nominationRole())));
        fundamentos.add(InstitutionalPanelProvisioningMessages.initialRoute(support.nullSafe(support.firstNonBlank(profile.landingPath(), snapshot.initialRoute()))));
        if (context.catalogEntry() != null) {
            fundamentos.add(InstitutionalPanelProvisioningMessages.catalogProfile(context.catalogEntry().codigo()));
        }
        if (!blueprintMatched) {
            findings.add(InstitutionalPanelProvisioningMessages.FINDING_BLUEPRINT);
        }
        if (!workspaceBound) {
            findings.add(InstitutionalPanelProvisioningMessages.FINDING_WORKSPACE);
        }
        if (!routeReady) {
            findings.add(InstitutionalPanelProvisioningMessages.FINDING_ROUTE);
        }
        if (!sectionsReady) {
            findings.add(InstitutionalPanelProvisioningMessages.FINDING_SECTIONS);
        }
        if (!quickActionsReady) {
            findings.add(InstitutionalPanelProvisioningMessages.FINDING_QUICK_ACTIONS);
        }
        if (!guardsReady) {
            findings.add(InstitutionalPanelProvisioningMessages.FINDING_GUARDS);
        }
        if (!visibilityRulesReady) {
            findings.add(InstitutionalPanelProvisioningMessages.FINDING_VISIBILITY);
        }
        if (!tabsReady) {
            findings.add(InstitutionalPanelProvisioningMessages.FINDING_TABS);
        }
        if (!workspaceActionsReady) {
            findings.add(InstitutionalPanelProvisioningMessages.FINDING_WORKSPACE_ACTIONS);
        }
        if (!authorityBandsReady) {
            findings.add(InstitutionalPanelProvisioningMessages.FINDING_AUTHORITY_BANDS);
        }
        if (!separatorsReady) {
            findings.add(InstitutionalPanelProvisioningMessages.FINDING_SEPARATORS);
        }
        if (structuralComplete) {
            fundamentos.add(InstitutionalPanelProvisioningMessages.PANEL_COMPLETE);
        }
        if (sharedExperienceReady) {
            fundamentos.add(InstitutionalPanelProvisioningMessages.SHARED_EXPERIENCE_COMPLETE);
        }

        return new InstitutionalPanelProvisioningReadiness(
                context.catalogEntry() == null ? null : context.catalogEntry().codigo(),
                profile.panelCode(),
                support.firstNonBlank(profile.landingPath(), snapshot.initialRoute()),
                blueprintMatched,
                workspaceBound,
                routeReady,
                sectionsReady,
                quickActionsReady,
                guardsReady,
                visibilityRulesReady,
                tabsReady,
                workspaceActionsReady,
                authorityBandsReady,
                separatorsReady,
                notificationsReady,
                calendarReady,
                hearingsReady,
                readingModeReady,
                triageReady,
                presentationReady,
                colorSystemReady,
                opinionFlowReady,
                calculatorReady,
                sharedExperienceReady,
                complete,
                context.blueprints().size(),
                snapshot.primarySections().size(),
                snapshot.quickActions().size(),
                snapshot.securityGuards().size(),
                snapshot.visibilityRules().size(),
                snapshot.tabs().size(),
                workspace == null ? 0 : workspace.actions().size(),
                workspace == null ? 0 : workspace.authorityBands().size(),
                workspace == null ? 0 : workspace.separators().size(),
                7,
                readySharedSurfaces.size(),
                List.copyOf(snapshot.primarySections()),
                List.copyOf(snapshot.quickActions()),
                List.copyOf(snapshot.securityGuards()),
                List.copyOf(snapshot.visibilityRules()),
                List.copyOf(snapshot.tabs()),
                List.copyOf(readySharedSurfaces),
                List.copyOf(missingSharedSurfaces),
                List.copyOf(findings),
                List.copyOf(fundamentos),
                hearingGovernance,
                deskGovernance,
                now
        );
    }

    private void registerSharedSurface(boolean ready,
                                       LinkedHashSet<String> readySharedSurfaces,
                                       LinkedHashSet<String> missingSharedSurfaces,
                                       String key,
                                       String route,
                                       LinkedHashSet<String> fundamentos,
                                       LinkedHashSet<String> findings,
                                       String missingFinding) {
        if (ready) {
            readySharedSurfaces.add(key);
            fundamentos.add(InstitutionalPanelProvisioningMessages.readySharedSurface(key));
            fundamentos.add(InstitutionalPanelProvisioningMessages.sharedSurface(key, route));
            return;
        }
        missingSharedSurfaces.add(key);
        findings.add(missingFinding);
    }
}
