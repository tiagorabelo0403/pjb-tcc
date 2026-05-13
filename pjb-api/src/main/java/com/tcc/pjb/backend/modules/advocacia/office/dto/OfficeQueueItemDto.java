package com.tcc.pjb.backend.modules.advocacia.office.dto;

import java.time.LocalDateTime;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeQueueStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficeQueueItemDto {
    private Long id;
    private Long equipeId;
    private Long executorUserId;
    private Long signerUserId;
    private OfficeActionType actionType;
    private String resourceType;
    private String resourceId;
    private OfficeQueueStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;
    private Long decidedByUserId;
    private String decisionReason;
    private String requestId;
    private String payloadHash;
    private String summary;
}
