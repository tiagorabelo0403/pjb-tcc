package com.tcc.pjb.backend.model.dto.publico;

import java.util.List;

public record PublicJulgamentosConsultaResponse(
        Long processoId,
        String numero,
        boolean publico,
        List<PublicJulgamentoAcordaoDto> julgamentosPublicados
) {
}
