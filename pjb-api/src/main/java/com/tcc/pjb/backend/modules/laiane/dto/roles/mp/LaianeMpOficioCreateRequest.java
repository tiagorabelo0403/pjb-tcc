package com.tcc.pjb.backend.modules.laiane.dto.roles.mp;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeMpOficioCreateRequest {

    
    private Long destinoId;

    @NotBlank
    private String tipo;

    private String protocolo;

    private String assunto;

    @NotBlank
    private String conteudo;

    
    private String justificativa;
}
