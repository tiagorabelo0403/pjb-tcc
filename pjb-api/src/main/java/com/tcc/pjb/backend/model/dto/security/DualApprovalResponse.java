package com.tcc.pjb.backend.model.dto.security;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DualApprovalResponse {
    private Long id;
    private String status;
    private String action;
    private String method;
    private String path;
    private String ruleId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Long requesterUserId;
    private Long requesterDeviceId;
    private Long equipeId;
    private Long approvedByUserId;
    private LocalDateTime approvedAt;
    private Long rejectedByUserId;
    private LocalDateTime rejectedAt;
}
