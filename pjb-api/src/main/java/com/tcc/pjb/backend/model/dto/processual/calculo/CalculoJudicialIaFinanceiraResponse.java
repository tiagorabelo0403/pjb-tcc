package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record CalculoJudicialIaFinanceiraResponse(
        String agente,
        String dominio,
        CalculoJudicialSolicitantePerfil perfilResolvido,
        String status,
        boolean calculoExecutado,
        String mensagemAbertura,
        String mensagemResultado,
        List<String> guardrails,
        List<String> pendencias,
        List<String> bloqueios,
        List<String> ajustesAplicados,
        List<String> confirmacoesRecomendadas,
        @Schema(description = "Resultado do auto-preenchimento aplicado pela IA financeira — chaves dependem do formulario do rito", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> autopreenchimentoAplicado,
        @Schema(description = "Metadados tecnicos da resposta de IA financeira — versao do agente, modo e estado de execucao", implementation = Object.class)
        @Size(max = 20)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metadata,
        CalculoJudicialAssistenciaResponse assistencia,
        CalculoJudicialResumoResponse resultado,
        Instant geradoEm
) {
}

