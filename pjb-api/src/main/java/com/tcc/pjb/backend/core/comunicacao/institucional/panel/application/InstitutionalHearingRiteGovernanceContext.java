package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import java.util.LinkedHashSet;
import java.util.List;

record InstitutionalHearingRiteGovernanceContext(
        InstitutionalOperationalProfileProjection profile,
        InstitutionalProcessWorkspace workspace,
        String scope,
        String schedulingScopeKey,
        List<String> topLevelSegregationGuards,
        LinkedHashSet<String> requestActors,
        LinkedHashSet<String> preparatoryActors,
        LinkedHashSet<String> communicationActors,
        LinkedHashSet<String> operationalActors,
        LinkedHashSet<String> trackingActors,
        LinkedHashSet<String> oversightActors,
        boolean sectionVisible,
        boolean canRequestHearing,
        boolean canSuggestSlot,
        boolean canOperationallySchedule,
        boolean canReschedule,
        boolean canCancel,
        boolean canReserveRoom,
        boolean canManageVirtualRoom,
        boolean canConfirmAttendance,
        boolean canRecordTerm,
        boolean canIssueHearingCommunications,
        boolean canPrepareHearingBundle,
        boolean requiresUnitIsolation,
        boolean requiresJudicialAuthorization,
        boolean requiresSecretariatCoordination,
        boolean legalInstitution,
        boolean secretariat,
        boolean scheduler,
        boolean technicalSupport,
        boolean management,
        boolean prisonFlow,
        boolean hybridJudicial,
        boolean federalScope,
        boolean laborScope,
        boolean electoralScope,
        boolean militaryScope,
        boolean militaryFederalScope,
        boolean broadCommonScope,
        boolean cejuscScope,
        boolean civilBroad,
        boolean juizadosBroad,
        boolean publicTreasuryBroad,
        boolean childhoodBroad,
        boolean penalBroad,
        boolean recursalBroad
) {
    InstitutionalHearingRiteGovernanceContext {
        topLevelSegregationGuards = safeList(topLevelSegregationGuards);
        requestActors = safeSet(requestActors);
        preparatoryActors = safeSet(preparatoryActors);
        communicationActors = safeSet(communicationActors);
        operationalActors = safeSet(operationalActors);
        trackingActors = safeSet(trackingActors);
        oversightActors = safeSet(oversightActors);
    }

    private static List<String> safeList(List<String> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return List.copyOf(source.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .distinct()
                .toList());
    }

    private static LinkedHashSet<String> safeSet(LinkedHashSet<String> source) {
        LinkedHashSet<String> copy = new LinkedHashSet<>();
        if (source == null || source.isEmpty()) {
            return copy;
        }
        source.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .forEach(copy::add);
        return copy;
    }
}
