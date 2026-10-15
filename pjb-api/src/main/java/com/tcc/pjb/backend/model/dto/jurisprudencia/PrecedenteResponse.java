package com.tcc.pjb.backend.model.dto.jurisprudencia;

import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoPrecedente;
import com.tcc.pjb.backend.model.entity.enums.TribunalFonte;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PrecedenteResponse(
        Long id,
        TribunalFonte fonte,
        TipoPrecedente tipo,
        String identificador,
        String titulo,
        String tese,
        String ementaResumo,
        String urlReferencia,
        LocalDate dataPublicacao,
        RamoDireito ramoSugerido,
        RitoProcessual ritoSugerido,
        LocalDateTime createdAt) {

    public static PrecedenteResponse de(Precedente precedente) {
        return new PrecedenteResponse(
                precedente.getId(),
                precedente.getFonte(),
                precedente.getTipo(),
                precedente.getIdentificador(),
                precedente.getTitulo(),
                precedente.getTese(),
                precedente.getEmentaResumo(),
                precedente.getUrlReferencia(),
                precedente.getDataPublicacao(),
                precedente.getRamoSugerido(),
                precedente.getRitoSugerido(),
                precedente.getCreatedAt());
    }
}
