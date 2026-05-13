package com.tcc.pjb.backend.model.dto.consultapublica;

import java.util.List;

public record ConsultaPublicaSearchConfigDto(
        int minQueryLength,
        int defaultPageSize,
        int maxPageSize,
        List<ConsultaPublicaFilterOptionDto> tiposJustica,
        List<ConsultaPublicaFilterOptionDto> ramosDireito,
        List<String> supportedKeys,
        String placeholder,
        String defaultSort
) {
}
