package com.tcc.pjb.backend.model.dto.triagem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.constraints.Size;

public record TriagemNacionalRequest(
        @Size(max = 80) String nupnProvisorio,
        @Size(max = 120) String classeTpuSugerida,
        @Size(max = 180) String assuntoTpuSugerido,
        @Size(max = 80) String ramoDireito,
        BigDecimal valorCausa,
        @Size(max = 6000) String textoFatosResumido,
        @Size(max = 20) String cpfCnpjAutor,
        @Size(max = 20) String cpfCnpjReu,
        @Size(max = 40) String oabAdvogado,
        @Size(max = 2) String ufAdvogado,
        List<@Size(max = 120) String> documentosAnexados,
        LocalDate dataFatoGerador,
        Boolean requerLiminar,
        Boolean atoJurisdicionalAnterior
) {
}
