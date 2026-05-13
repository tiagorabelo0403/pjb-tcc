package com.tcc.pjb.backend.model.dto.workspace.fila;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import com.tcc.pjb.backend.model.dto.workspace.localizador.WorkspaceLocalizadorCriteria;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceFilaUpdateRequest {

    @NotBlank
    private String nome;

    private String descricao;

    private Integer orderIndex;

    @Valid
    private WorkspaceLocalizadorCriteria processoCriteria;

    @Valid
    private WorkspaceFilaWorkItemCriteria workItemCriteria;
}
