package com.tcc.pjb.backend.model.dto.workspace.localizador;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceProcessoResumoResponse {

    private Long processoId;
    private String numeroUnificado;
    private String classeProcessual;
    private String assunto;
    private StatusProcesso status;
    private FaseProcessual fase;
    private RitoProcessual rito;
    private LocalDateTime dataUltimaMovimentacao;

    private List<WorkspaceEtiquetaLite> etiquetas;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkspaceEtiquetaLite {
        private UUID id;
        private String nome;
        private String corHex;
    }
}
