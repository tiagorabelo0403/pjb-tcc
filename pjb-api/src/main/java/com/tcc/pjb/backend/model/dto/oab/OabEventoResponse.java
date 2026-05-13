package com.tcc.pjb.backend.model.dto.oab;

import java.time.LocalDateTime;
import java.util.List;
import com.tcc.pjb.backend.model.entity.enums.StatusEventoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoEventoInstitucional;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OabEventoResponse {

    private Long id;
    private TipoEventoInstitucional tipo;
    private StatusEventoInstitucional status;
    private String uf;
    private String tribunal;
    private String orgao;
    private Long processoId;
    private String numeroProcesso;
    private int severidade;
    private String resumo;
    private String detalhes;
    private String criadoPor;
    private LocalDateTime criadoEm;

    private List<OabProvidenciaResponse> providencias;
}
