package com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain;

import java.util.List;
import java.util.Objects;

public record InstitutionalHearingSchedulingGovernance(
        boolean sectionVisible,
        boolean canRequestHearing,
        boolean canSuggestSlot,
        boolean canOrganizeDocket,
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
        String schedulingScopeKey,
        List<String> operationalQueues,
        List<String> segregationGuards,
        List<String> oversightActors,
        List<String> allowedRiteGroups,
        List<InstitutionalHearingRiteGovernance> riteGovernances,
        List<String> forbiddenActs,
        List<String> findings,
        List<String> fundamentos
) {
    public InstitutionalHearingSchedulingGovernance {
        Objects.requireNonNull(schedulingScopeKey);
        Objects.requireNonNull(operationalQueues);
        Objects.requireNonNull(segregationGuards);
        Objects.requireNonNull(oversightActors);
        Objects.requireNonNull(allowedRiteGroups);
        Objects.requireNonNull(riteGovernances);
        Objects.requireNonNull(forbiddenActs);
        Objects.requireNonNull(findings);
        Objects.requireNonNull(fundamentos);
    }
}
