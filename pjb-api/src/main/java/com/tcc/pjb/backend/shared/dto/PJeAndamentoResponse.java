package com.tcc.pjb.backend.shared.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PJeAndamentoResponse(
        String numeroProcessoTribunal,
        String status,
        Instant consultadoEm,
        String correlationId,
        List<Andamento> andamentos,
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
            Map<String, Object> extras
    ) {

        public Andamento {
            extras = extras == null ? Map.of() : Map.copyOf(extras);
        }
    }
}
