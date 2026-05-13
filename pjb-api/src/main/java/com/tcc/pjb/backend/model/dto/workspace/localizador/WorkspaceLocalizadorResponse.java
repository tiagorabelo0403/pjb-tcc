package com.tcc.pjb.backend.model.dto.workspace.localizador;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceLocalizadorResponse {
    private UUID id;
    private String nome;
    private String descricao;
    private WorkspaceLocalizadorCriteria criteria;
    private boolean compartilhado;
    private Long ownerUserId;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
