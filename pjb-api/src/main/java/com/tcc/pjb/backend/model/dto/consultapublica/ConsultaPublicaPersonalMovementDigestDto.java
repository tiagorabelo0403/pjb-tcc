package com.tcc.pjb.backend.model.dto.consultapublica;

import java.util.List;

public record ConsultaPublicaPersonalMovementDigestDto(
        int totalItems,
        long recent48h,
        long withOpenDeadline,
        long blockedItems,
        String dominantStatus,
        List<String> topSignals
) {
}
