package com.tcc.pjb.backend.core.procedural;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProceduralJurisdictionIntakeReport(
        Instant generatedAt,
        String branchProfile,
        String filingTier,
        String competenceDefinitionMode,
        String territorialAnchorMode,
        String defaultEntryMode,
        String intakeMode,
        String questionStrategy,
        boolean firstInstanceDefault,
        boolean mayStartAtTribunal,
        boolean userMayChooseTribunal,
        boolean userMayChooseForum,
        boolean userMayChooseJudicialUnit,
        boolean manualHintsProvided,
        boolean technicalSelectionOptional,
        boolean noviceSafe,
        List<String> mandatorySignals,
        List<String> territorialSignals,
        List<String> institutionalSignals,
        List<String> distributionRules,
        List<String> warnings,
        List<Map<String, Object>> guidedQuestions,
        List<Map<String, Object>> ambiguityQuestions,
        Map<String, Object> resolutionPolicy,
        Map<String, Object> metadata
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("branchProfile", branchProfile);
        out.put("filingTier", filingTier);
        out.put("competenceDefinitionMode", competenceDefinitionMode);
        out.put("territorialAnchorMode", territorialAnchorMode);
        out.put("defaultEntryMode", defaultEntryMode);
        out.put("intakeMode", intakeMode);
        out.put("questionStrategy", questionStrategy);
        out.put("firstInstanceDefault", firstInstanceDefault);
        out.put("mayStartAtTribunal", mayStartAtTribunal);
        out.put("userMayChooseTribunal", userMayChooseTribunal);
        out.put("userMayChooseForum", userMayChooseForum);
        out.put("userMayChooseJudicialUnit", userMayChooseJudicialUnit);
        out.put("manualHintsProvided", manualHintsProvided);
        out.put("technicalSelectionOptional", technicalSelectionOptional);
        out.put("noviceSafe", noviceSafe);
        out.put("mandatorySignals", mandatorySignals == null ? List.of() : mandatorySignals);
        out.put("territorialSignals", territorialSignals == null ? List.of() : territorialSignals);
        out.put("institutionalSignals", institutionalSignals == null ? List.of() : institutionalSignals);
        out.put("distributionRules", distributionRules == null ? List.of() : distributionRules);
        out.put("warnings", warnings == null ? List.of() : warnings);
        out.put("guidedQuestions", sanitizeQuestionMaps(guidedQuestions));
        out.put("ambiguityQuestions", sanitizeQuestionMaps(ambiguityQuestions));
        LinkedHashMap<String, Object> safeResolutionPolicy = resolutionPolicy == null ? new LinkedHashMap<>() : new LinkedHashMap<>(resolutionPolicy);
        safeResolutionPolicy.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        out.put("resolutionPolicy", safeResolutionPolicy);
        LinkedHashMap<String, Object> safeMetadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
        safeMetadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        out.put("metadata", safeMetadata);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Collections.unmodifiableMap(out);
    }

    private List<Map<String, Object>> sanitizeQuestionMaps(List<Map<String, Object>> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream().map(this::sanitizeQuestionMap).toList();
    }

    private Map<String, Object> sanitizeQuestionMap(Map<String, Object> source) {
        LinkedHashMap<String, Object> safe = source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
        safe.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Map.copyOf(safe);
    }
}
