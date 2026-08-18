package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record CalculoJudicialTabelaOficialResponse(
        String version,
        String fingerprint,
        String dominioFiltrado,
        Map<String, String> rotas,
        List<CalculoJudicialTabelaOficialItemResponse> tabelas,
        @Schema(description = "Politica de atualizacao da tabela oficial — frequencia, fonte e criterios de validacao por orgao emissor", implementation = Object.class)
        @Size(max = 20)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> politicaAtualizacao,
        Instant geradoEm
) {
}

