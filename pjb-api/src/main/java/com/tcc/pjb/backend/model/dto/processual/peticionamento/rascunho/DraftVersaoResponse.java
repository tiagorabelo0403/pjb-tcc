package com.tcc.pjb.backend.model.dto.processual.peticionamento.rascunho;

import com.tcc.pjb.backend.model.entity.peticionamento.PeticaoDraftVersao;
import java.time.Instant;

public record DraftVersaoResponse(
        int versaoSeq,
        String origem,
        String tituloCaso,
        String hashIntegridade,
        int tamanhoMinuta,
        Instant createdAt
) {
    public static DraftVersaoResponse from(PeticaoDraftVersao v) {
        return new DraftVersaoResponse(
                v.getVersaoSeq(),
                v.getOrigem(),
                v.getTituloCaso(),
                v.getHashIntegridade(),
                v.getMinutaHtml() == null ? 0 : v.getMinutaHtml().length(),
                v.getCreatedAt());
    }
}
