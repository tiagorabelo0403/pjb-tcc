package com.tcc.pjb.backend.model.dto.advogado;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;

public record LaianePeticaoInicialEstruturarRequest(
        Long processoId,
        @NotBlank String tituloCaso,
        String parteAutora,
        String parteRe,
        String ramoDireito,
        String ritoProcessual,
        String classeProcessual,
        List<String> fatos,
        List<String> fundamentosJuridicos,
        List<String> pedidos,
        List<String> provasIndicadas,
        BigDecimal valorCausa,
        boolean tutelaUrgencia
) {
}
