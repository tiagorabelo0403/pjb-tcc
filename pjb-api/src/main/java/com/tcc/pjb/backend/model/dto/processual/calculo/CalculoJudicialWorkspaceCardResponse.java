package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record CalculoJudicialWorkspaceCardResponse(
        String codigo,
        String titulo,
        String descricao,
        String aba,
        List<String> perfisPermitidos,
        List<String> mensagensAjuda,
        List<String> secoes,
        List<String> automacoesSeguras,
        Map<String, String> rotas,
        @Schema(description = "Configuracao visual do card de calculo — hints de layout e tipografia por tema institucional", implementation = Object.class)
        @Size(max = 20)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> design
) {
}

