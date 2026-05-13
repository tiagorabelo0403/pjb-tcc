package com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeDeadlineDelegationCreateRequest {
    @NotNull
    private Long workItemId;
    @NotNull
    private Long delegateeId;
    private String descricao;
    
    private String justificativa;
}
