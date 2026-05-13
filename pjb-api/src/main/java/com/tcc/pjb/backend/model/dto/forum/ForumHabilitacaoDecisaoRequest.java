package com.tcc.pjb.backend.model.dto.forum;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumHabilitacaoDecisaoRequest {

    
    @NotBlank
    private String motivo;
}
