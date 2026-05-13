package com.tcc.pjb.backend.model.dto.oab;

import java.time.LocalDateTime;
import com.tcc.pjb.backend.model.entity.enums.TipoProvidenciaInstitucional;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OabProvidenciaResponse {

    private Long id;
    private TipoProvidenciaInstitucional tipo;
    private String titulo;
    private String descricao;
    private String criadoPor;
    private LocalDateTime criadoEm;
}
