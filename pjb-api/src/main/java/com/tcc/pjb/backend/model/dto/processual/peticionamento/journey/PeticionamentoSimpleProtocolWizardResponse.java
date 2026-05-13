package com.tcc.pjb.backend.model.dto.processual.peticionamento.journey;

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
        Map<String, Object> protocolPreview,
        Map<String, Object> playbook,
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
