package com.tcc.pjb.backend.model.dto.publico;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

public record PublicJulgamentosConsultaResponse(
        @JsonIgnore Long processoId,
        String numero,
        boolean publico,
        List<PublicJulgamentoAcordaoDto> julgamentosPublicados
) {
}
