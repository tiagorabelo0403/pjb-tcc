package com.tcc.pjb.backend.model.dto.workspace.fila;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.tcc.pjb.backend.model.dto.workspace.localizador.WorkspaceLocalizadorCriteria;
import com.tcc.pjb.backend.model.entity.workspace.WorkspaceFilaKind;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceFilaCreateRequest {

    @NotBlank
    private String nome;

    private String descricao;

    @NotNull
    private WorkspaceFilaKind kind;

    private Integer orderIndex;

    @Valid
    private WorkspaceLocalizadorCriteria processoCriteria;

    @Valid
    private WorkspaceFilaWorkItemCriteria workItemCriteria;
}
