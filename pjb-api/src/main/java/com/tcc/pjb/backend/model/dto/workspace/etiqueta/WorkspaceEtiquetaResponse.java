package com.tcc.pjb.backend.model.dto.workspace.etiqueta;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceEtiquetaResponse {
    private UUID id;
    private String nome;
    private String corHex;
    private boolean sistema;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
