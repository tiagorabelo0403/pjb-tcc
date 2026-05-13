package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.util.List;

public record NationalCommunicationInstitutionalHearingSchedulingGovernanceResponse(
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
        List<NationalCommunicationInstitutionalHearingRiteGovernanceResponse> riteGovernances,
        List<String> forbiddenActs,
        List<String> findings,
        List<String> fundamentos
) {
}
