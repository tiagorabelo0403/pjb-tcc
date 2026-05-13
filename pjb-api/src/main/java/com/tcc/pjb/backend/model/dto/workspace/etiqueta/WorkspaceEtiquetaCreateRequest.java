package com.tcc.pjb.backend.model.dto.workspace.etiqueta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceEtiquetaCreateRequest {

    @NotBlank(message = "nome é obrigatório")
    @Size(max = 80, message = "nome deve ter no máximo 80 caracteres")
    private String nome;

    
    @Pattern(regexp = "^#?[0-9a-fA-F]{3}([0-9a-fA-F]{3})?$", message = "corHex inválida (use #RRGGBB ou #RGB)")
    private String corHex;
}
