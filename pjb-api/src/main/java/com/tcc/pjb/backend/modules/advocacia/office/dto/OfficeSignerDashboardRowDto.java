package com.tcc.pjb.backend.modules.advocacia.office.dto;

import java.time.LocalDate;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeDelegationMode;

public record OfficeSignerDashboardRowDto(
        LocalDate day,
        Long executorUserId,
        OfficeDelegationMode mode,
        long total
) {
}
