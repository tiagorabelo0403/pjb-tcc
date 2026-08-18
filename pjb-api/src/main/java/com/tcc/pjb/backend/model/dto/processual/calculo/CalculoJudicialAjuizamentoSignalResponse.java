package com.tcc.pjb.backend.model.dto.processual.calculo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CalculoJudicialAjuizamentoSignalResponse(
        String status,
        boolean requerCalculo,
        String dominioSugerido,
        @Schema(description = "Mensagens transitórias do pipeline de ajuizamento — heterogêneas por agente, limpas após processamento",
                implementation = Object.class)
        @Size(max = 20)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<Map<String, Object>> mensagensTemporarias,
        List<String> recomendacoes,
        List<String> bloqueios,
        @Schema(description = "Rotas de rito disponíveis para ajuizamento — varia por classificação processual e competência",
                implementation = Object.class)
        @Size(max = 20)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> routes,
        @Schema(description = "Snapshot de referências econômicas no momento do ajuizamento — contém valores e metadados aninhados",
                implementation = Object.class)
        @Size(max = 15)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> economicReferences,
        @Schema(description = "Resultados dos agentes de IA — payload polimórfico por tipo de agente (financeiro/compliance/tributário)",
                implementation = Object.class)
        @Size(max = 10)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> agentResults,
        @Schema(description = "Metadados técnicos do sinal de ajuizamento — timestamp, versão do pipeline e modo de operação",
                implementation = Object.class)
        @Size(max = 15)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metadata,
        Instant geradoEm
) {
}
