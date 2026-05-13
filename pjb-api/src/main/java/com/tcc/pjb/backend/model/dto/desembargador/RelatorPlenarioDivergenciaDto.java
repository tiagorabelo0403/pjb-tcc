package com.tcc.pjb.backend.model.dto.desembargador;

import java.util.List;

public record RelatorPlenarioDivergenciaDto(
        String eixo,
        String referencia,
        List<String> magistrados
) {
}
