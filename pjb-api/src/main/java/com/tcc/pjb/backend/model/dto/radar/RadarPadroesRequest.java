package com.tcc.pjb.backend.model.dto.radar;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RadarPadroesRequest(
        Long processoId,
        String nupn,
        String documentoAutor,
        String documentoReu,
        String escritorioOab,
        String tribunalCodigo,
        String ramoDireito,
        String classeProcessual,
        String assunto,
        BigDecimal valorCausa,
        String resumoFatos,
        LocalDate dataAjuizamento,
        String statusProcesso,
        String resultadoFinal
) {
}
