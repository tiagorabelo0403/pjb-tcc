package com.tcc.pjb.backend.shared.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record PJeAndamentoResponse(
        String numeroProcessoTribunal,
        String status,
        Instant consultadoEm,
        String correlationId,
        List<Andamento> andamentos,
        @Schema(description = "Metadados e dados adicionais do andamento PJe — estrutura opaca do sistema legado", implementation = Object.class)
    @Size(max = 30)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    Map<String, Object> metadados
) {

    public PJeAndamentoResponse {
        andamentos = andamentos == null ? List.of() : List.copyOf(andamentos);
        metadados = metadados == null ? Map.of() : Map.copyOf(metadados);
    }

    public record Andamento(
            Instant data,
            String descricao,
            String orgao,
            @Schema(description = "Dados extras do andamento PJe — estrutura opaca do sistema legado", implementation = Object.class)
            @Size(max = 20)
            @JsonInclude(JsonInclude.Include.NON_EMPTY)
            Map<String, Object> extras
    ) {

        public Andamento {
            extras = extras == null ? Map.of() : Map.copyOf(extras);
        }
    }
}

