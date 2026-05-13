package com.tcc.pjb.backend.modules.advocacia.office.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeDelegationMode;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeQueueStatus;

public record OfficeDelegatedActionOpsDto(
        Long id,
        LocalDateTime createdAt,
        OfficeDelegationMode mode,
        OfficeActionType actionType,
        Long executorUserId,
        Long signerUserId,
        String resourceType,
        String resourceId,
        OfficeQueueStatus queueStatus,
        UUID jobId,
        String requestId
) {
}
