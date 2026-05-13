package com.tcc.pjb.backend.model.dto.teto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TetoProcessualDiagnosticoRequest(
        BigDecimal valorCausa,
        String tipoJustica,
        String ramoDireito,
        String ritoProcessual,
        LocalDate dataReferencia,
        String classeProcessual,
        String assunto
) {
}
