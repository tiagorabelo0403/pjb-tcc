package com.tcc.pjb.backend.modules.laiane.dto.protocol;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeProtocolPackageDto {
    private Long id;
    private String title;
    private String integrityHash;
    private String status;
    private LocalDateTime createdAt;
    private Long equipeId;
    private Long executorUserId;
    private Long signerUserId;
    private Long officeQueueItemId;
    private UUID submissionJobId;
    private String externalProtocolRef;
    private LocalDateTime submittedAt;
    private String lastError;
    private String guardrailStatus;
    private Boolean readyForSubmission;
    private String guardrailNextAction;
    private List<String> guardrailBlockers;
}
