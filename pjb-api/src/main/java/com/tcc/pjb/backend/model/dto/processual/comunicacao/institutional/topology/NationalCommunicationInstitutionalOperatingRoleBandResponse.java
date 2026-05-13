package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology;

import java.util.List;

public record NationalCommunicationInstitutionalOperatingRoleBandResponse(
        String bandKey,
        String laneKind,
        String nominationRole,
        String tipoUsuario,
        String displayName,
        long activeNominations,
        boolean judicialAuthority,
        boolean institutionalOnly,
        boolean personalDirectEntryAllowed,
        List<String> capacities,
        List<String> fundamentos
) {
}
