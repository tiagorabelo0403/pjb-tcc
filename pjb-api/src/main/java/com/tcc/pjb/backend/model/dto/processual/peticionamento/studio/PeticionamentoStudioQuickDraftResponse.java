package com.tcc.pjb.backend.model.dto.processual.peticionamento.studio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record PeticionamentoStudioQuickDraftResponse(
        String status,
        String sessionKey,
        String actorProfile,
        String title,
        String draftingMode,
        String markdown,
        @Schema(description = "Contexto procedimental — classeProcessual, ramoDireito, ritoProcessual, justicaSugerida, petitionFamily (Categoria D: passado de projection)")
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> procedure,
        @Schema(description = "Resumo de evidências do caso — estrutura varia por tipo de prova (Categoria D)")
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> evidence,
        @Schema(description = "Linha do tempo do caso — eventos e marcos processuais (Categoria D)")
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> caseTimeline,
        @Schema(description = "Matriz de requerimento de provas — varia por rito e tipo de prova (Categoria D)")
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> proofRequestMatrix,
        @Schema(description = "Checklist de protocolo — itens de conformidade procedimental (Categoria D)")
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> protocolChecklist,
        @Schema(description = "Matriz de risco — checklist, blockingIssues, alerts (Categoria D)")
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> riskMatrix,
        @Schema(description = "Matriz de gaps documentais — documentos faltantes por tipo (Categoria D)")
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> documentGapMatrix,
        @Schema(description = "Governança de revisão — regras e aprovadores do processo (Categoria D)")
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> reviewGovernance,
        List<String> checklist,
        List<String> nextSteps
) {
    public PeticionamentoStudioQuickDraftResponse {
        status = normalize(status, "EM_EDICAO");
        sessionKey = normalize(sessionKey, "SEM_SESSAO");
        actorProfile = normalize(actorProfile, "PETICIONANTE_AUTENTICADO");
        title = normalize(title, "PETIÇÃO RÁPIDA");
        draftingMode = normalize(draftingMode, "RAPIDO_ASSISTIDO");
        markdown = normalize(markdown, "# Petição em elaboração\n");
        procedure = copyOfMap(procedure);
        evidence = copyOfMap(evidence);
        caseTimeline = copyOfMap(caseTimeline);
        proofRequestMatrix = copyOfMap(proofRequestMatrix);
        protocolChecklist = copyOfMap(protocolChecklist);
        riskMatrix = copyOfMap(riskMatrix);
        documentGapMatrix = copyOfMap(documentGapMatrix);
        reviewGovernance = copyOfMap(reviewGovernance);
        checklist = checklist == null ? List.of() : List.copyOf(checklist);
        nextSteps = nextSteps == null ? List.of() : List.copyOf(nextSteps);
    }

    private static Map<String, Object> copyOfMap(Map<String, Object> value) {
        return value == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(value));
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}

