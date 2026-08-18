package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record CalculoJudicialResumoResponse(
        String dominio,
        String titulo,
        String numeroProcesso,
        CalculoJudicialSolicitantePerfil perfilSolicitante,
        String narrativaPrincipal,
        String narrativaTecnica,
        BigDecimal subtotalPrincipal,
        BigDecimal subtotalAtualizacao,
        BigDecimal subtotalAcessorios,
        BigDecimal totalGeral,
        List<CalculoJudicialItemResponse> itens,
        List<String> alertas,
        List<String> fundamentos,
        List<String> trilhaAuditoria,
        @Schema(description = "Metadados tecnicos do resumo de calculo — versao, modo de operacao e trilha de auditoria", implementation = Object.class)
        @Size(max = 20)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metadata,
        Instant geradoEm
) {
}

