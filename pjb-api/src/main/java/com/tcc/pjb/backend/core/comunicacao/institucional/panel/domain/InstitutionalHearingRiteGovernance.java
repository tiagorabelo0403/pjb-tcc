package com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain;

import java.util.List;
import java.util.Objects;

public record InstitutionalHearingRiteGovernance(
        String riteCode,
        String justiceBranch,
        String jurisdictionAxis,
        String specializationAxis,
        String hearingKind,
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
        boolean canOnlyTrack,
        boolean requiresUnitIsolation,
        boolean requiresJudicialAuthorization,
        boolean requiresSecretariatCoordination,
        String queueScopeKey,
        List<String> allowedActs,
        List<String> forbiddenActs,
        List<String> requestActors,
        List<String> preparatoryActors,
        List<String> communicationActors,
        List<String> operationalActors,
        List<String> trackingActors,
        List<String> oversightActors,
        List<String> segregationGuards,
        List<String> fundamentos
) {
    public InstitutionalHearingRiteGovernance {
        Objects.requireNonNull(riteCode);
        Objects.requireNonNull(justiceBranch);
        Objects.requireNonNull(jurisdictionAxis);
        Objects.requireNonNull(specializationAxis);
        Objects.requireNonNull(hearingKind);
        Objects.requireNonNull(queueScopeKey);
        Objects.requireNonNull(allowedActs);
        Objects.requireNonNull(forbiddenActs);
        Objects.requireNonNull(requestActors);
        Objects.requireNonNull(preparatoryActors);
        Objects.requireNonNull(communicationActors);
        Objects.requireNonNull(operationalActors);
        Objects.requireNonNull(trackingActors);
        Objects.requireNonNull(oversightActors);
        Objects.requireNonNull(segregationGuards);
        Objects.requireNonNull(fundamentos);
    }
}
