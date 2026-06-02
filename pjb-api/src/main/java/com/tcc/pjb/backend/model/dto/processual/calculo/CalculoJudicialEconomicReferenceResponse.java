package com.tcc.pjb.backend.model.dto.processual.calculo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tcc.pjb.backend.model.dto.shared.calculo.CalculoJudicialInssReferenceDto;
import com.tcc.pjb.backend.model.dto.shared.calculo.CalculoJudicialSalarioMinimoDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;

public record CalculoJudicialEconomicReferenceResponse(
        String referenciaTemporal,
        CalculoJudicialSalarioMinimoDto salarioMinimoNacional,
        CalculoJudicialInssReferenceDto inss,
        @Schema(description = "URLs das fontes oficiais de referência econômica — Planalto, INSS, CNJ e manuais")
        @Size(max = 10)
        Map<String, String> fontesOficiais,
        @Schema(description = "Metadados técnicos do serviço de referência econômica — modo de refresh, versão e estado do painel",
                implementation = Object.class)
        @Size(max = 10)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metadata,
        Instant geradoEm
) {
}
