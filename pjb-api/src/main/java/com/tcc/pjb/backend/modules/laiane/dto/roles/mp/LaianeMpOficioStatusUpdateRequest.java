package com.tcc.pjb.backend.modules.laiane.dto.roles.mp;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeMpOficioStatusUpdateRequest {
    @NotBlank
    private String status;
    private String justificativa;
}
