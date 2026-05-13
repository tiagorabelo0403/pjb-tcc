package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.util.List;

public record NationalCommunicationInstitutionalHearingRiteGovernanceResponse(
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
}
