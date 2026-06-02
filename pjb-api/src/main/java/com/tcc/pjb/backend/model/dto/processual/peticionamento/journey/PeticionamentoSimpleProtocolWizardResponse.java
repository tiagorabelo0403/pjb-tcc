package com.tcc.pjb.backend.model.dto.processual.peticionamento.journey;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PeticionamentoSimpleProtocolWizardResponse(
        Instant generatedAt,
        String status,
        String rito,
        String ramo,
        String grupo,
        String tribunalCodigo,
        String tribunalNome,
        String judicialSystem,
        boolean readyForAssistedProtocol,
        boolean readyForRealConnector,
        boolean stepUpRequired,
        boolean certificateRequired,
        String nextAction,
        List<PeticionamentoSimpleProtocolWizardStepResponse> steps,
        List<String> blockingIssues,
        List<String> checklist,
        List<String> warnings,
        @Schema(description = "Preview dinâmico do protocolo — varia por tribunal e rito (Categoria D)")
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> protocolPreview,
        @Schema(description = "Playbook do rito processual — passos e requisitos por classificação (Categoria D)")
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> playbook,
        @Schema(description = "Variação por tribunal — configurações específicas do sistema judicial (Categoria D)")
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> tribunalVariation,
        List<String> nextSteps,
        PeticionamentoJourneyIntelligenceResponse journeyIntelligence
) {
    public PeticionamentoSimpleProtocolWizardResponse {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        status = normalize(status, "EM_EDICAO");
        rito = normalize(rito, "COMUM_ORDINARIO");
        ramo = normalize(ramo, "CIVIL");
        grupo = normalize(grupo, "CIVIL");
        tribunalCodigo = normalize(tribunalCodigo, "PJB_PADRAO");
        tribunalNome = normalize(tribunalNome, tribunalCodigo);
        judicialSystem = normalize(judicialSystem, "OUTRO");
        nextAction = normalize(nextAction, "REVISAR_PLAYBOOK_E_PROTOCOLAR");
        steps = steps == null ? List.of() : List.copyOf(steps);
        blockingIssues = blockingIssues == null ? List.of() : List.copyOf(blockingIssues);
        checklist = checklist == null ? List.of() : List.copyOf(checklist);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        protocolPreview = copyOfMap(protocolPreview);
        playbook = copyOfMap(playbook);
        tribunalVariation = copyOfMap(tribunalVariation);
        nextSteps = nextSteps == null ? List.of() : List.copyOf(nextSteps);
        journeyIntelligence = journeyIntelligence == null ? new PeticionamentoJourneyIntelligenceResponse(generatedAt, rito, ramo, "TRIAGEM", "ARRANQUE", 0, 0, 0, true, true, false, false, false, false, false, List.of(), List.of(), List.of(), List.of(), Map.of()) : journeyIntelligence;
    }

    private static Map<String, Object> copyOfMap(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        value.forEach((key, entry) -> {
            if (key != null) {
                out.put(key, entry);
            }
        });
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
