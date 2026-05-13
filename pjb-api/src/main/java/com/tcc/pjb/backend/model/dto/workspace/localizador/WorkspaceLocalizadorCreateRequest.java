package com.tcc.pjb.backend.model.dto.workspace.localizador;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceLocalizadorCreateRequest {

    @NotBlank(message = "nome é obrigatório")
    @Size(max = 120, message = "nome deve ter no máximo 120 caracteres")
    private String nome;

    @Size(max = 400, message = "descricao deve ter no máximo 400 caracteres")
    private String descricao;

    @NotNull(message = "criteria é obrigatório")
    @Valid
    private WorkspaceLocalizadorCriteria criteria;

    private Boolean compartilhado;
}
