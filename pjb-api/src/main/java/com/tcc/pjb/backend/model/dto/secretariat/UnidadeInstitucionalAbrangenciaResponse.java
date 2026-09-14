package com.tcc.pjb.backend.model.dto.secretariat;

import com.tcc.pjb.backend.model.entity.UnidadeInstitucionalAbrangencia;

public record UnidadeInstitucionalAbrangenciaResponse(
        Long id,
        Long unidadeInstitucionalId,
        String comarcaAtendida
) {
    public static UnidadeInstitucionalAbrangenciaResponse de(UnidadeInstitucionalAbrangencia abrangencia) {
        return new UnidadeInstitucionalAbrangenciaResponse(
                abrangencia.getId(),
                abrangencia.getUnidadeInstitucionalId(),
                abrangencia.getComarcaAtendida());
    }
}
