package com.tcc.pjb.backend.model.dto.processual.integration.intertribunal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record LitispendenciaIntertribunalRequest(
        String nupnProvisorio,
        String classeTpuSugerida,
        String assuntoTpuSugerido,
        String ramoDireito,
        BigDecimal valorCausa,
        String textoFatosResumido,
        String cpfCnpjAutor,
        String cpfCnpjReu,
        String oabAdvogado,
        String ufAdvogado,
        List<String> documentosAnexados,
        LocalDate dataFatoGerador,
        boolean requerLiminar,
        boolean atoJurisdicionalAnterior,
        Long processoId
) {
}
