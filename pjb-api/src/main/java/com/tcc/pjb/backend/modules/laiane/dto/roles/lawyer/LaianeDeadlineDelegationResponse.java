package com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeDeadlineDelegationResponse {
    private Long id;
    private Long delegatorId;
    private Long delegateeId;
    private Long workItemId;
    private Long processoId;
    private String status;
    private String descricao;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
