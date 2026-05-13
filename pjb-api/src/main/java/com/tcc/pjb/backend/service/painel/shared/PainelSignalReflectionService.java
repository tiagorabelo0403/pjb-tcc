package com.tcc.pjb.backend.service.painel.shared;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PainelSignalReflectionService {

    public Map<String, Object> deriveSignals(String panelCode,
                                             Map<String, Object> sharedExperience,
                                             int pendingCount,
                                             int urgentDeadlines,
                                             String dominantContext) {
        Map<String, Object> safeSharedExperience = map(sharedExperience);
        Map<String, Object> calendar = map(safeSharedExperience.get("calendar"));
        Map<String, Object> deadlines = map(safeSharedExperience.get("deadlines"));
        Map<String, Object> colors = map(safeSharedExperience.get("colors"));
        Map<String, Object> calculator = map(safeSharedExperience.get("calculator"));
        Map<String, Object> reading = map(safeSharedExperience.get("reading"));
        List<String> nativeSectionOrder = suggestedNativeSections(panelCode);
        int score = attentionScore(pendingCount, urgentDeadlines);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("panelCode", string(panelCode));
        out.put("dominantContext", string(dominantContext));
        out.put("priorityTag", resolvePriorityTag(pendingCount, urgentDeadlines));
        out.put("calendarLane", string(calendar.getOrDefault("recommendedLane", "GERAL")));
        out.put("calendarFocus", string(calendar.getOrDefault("recommendedFocus", "ROTINA_INSTITUCIONAL")));
        out.put("deadlineProfile", string(deadlines.getOrDefault("recommendedProfile", "OPERACIONAL_GERAL")));
        out.put("readabilityMode", string(reading.getOrDefault("recommendedMode", "TECNICO_ESTRUTURADO")));
        out.put("persona", string(colors.getOrDefault("recommendedPersona", "INSTITUCIONAL")));
        out.put("statusTags", list(colors.get("statusTags")));
        out.put("calculatorEnabled", bool(calculator.get("enabled")));
        out.put("calculatorDomains", list(calculator.get("preferredDomains")));
        out.put("calculatorExperienceMode", string(calculator.getOrDefault("experienceMode", "STANDARD")));
        out.put("suggestedNativeSections", nativeSectionOrder);
        out.put("nativeSectionOrder", nativeSectionOrder);
        out.put("highlightedNativeSection", nativeSectionOrder.isEmpty() ? "PAINEL" : nativeSectionOrder.getFirst());
        out.put("compositionMode", resolveCompositionMode(pendingCount, urgentDeadlines));
        out.put("collapseSecondaryBlocks", shouldCollapseSecondaryBlocks(pendingCount, urgentDeadlines));
        out.put("listOrderingMode", urgentDeadlines > 0 ? "URGENT_FIRST" : (pendingCount >= 15 ? "PRIORITY_FIRST" : "BALANCED"));
        out.put("nativeSectionPriorities", sectionPriorityMap(nativeSectionOrder));
        out.put("attentionScore", score);
        out.put("signalVersion", "PJB_SIGNAL_REFLECTION_V3");
        return Collections.unmodifiableMap(out);
    }

    public Map<String, Object> reflectInBlock(String panelCode,
                                              String blockName,
                                              Map<String, Object> nativeBlock,
                                              Map<String, Object> signals) {
        Map<String, Object> safeSignals = map(signals);
        LinkedHashMap<String, Object> out = nativeBlock == null ? new LinkedHashMap<>() : new LinkedHashMap<>(nativeBlock);
        LinkedHashMap<String, Object> reflection = new LinkedHashMap<>();
        reflection.put("panelCode", string(panelCode));
        reflection.put("blockName", string(blockName));
        reflection.put("priorityTag", string(safeSignals.getOrDefault("priorityTag", "NORMAL")));
        reflection.put("dominantContext", string(safeSignals.getOrDefault("dominantContext", "PAINEL")));
        reflection.put("compositionMode", string(safeSignals.getOrDefault("compositionMode", "BALANCED_FLOW")));
        reflection.put("collapseSecondaryBlocks", bool(safeSignals.get("collapseSecondaryBlocks")));
        reflection.put("listOrderingMode", string(safeSignals.getOrDefault("listOrderingMode", "BALANCED")));
        reflection.put("displayOrder", resolveDisplayOrder(blockName, safeSignals));
        reflection.put("displayPriority", resolveDisplayPriority(blockName, safeSignals));
        reflection.put("highlighted", isHighlightedBlock(blockName, safeSignals));
        switch (string(blockName)) {
            case "VISUAL_IDENTITY", "BRANDING" -> {
                reflection.put("persona", string(safeSignals.getOrDefault("persona", "INSTITUCIONAL")));
                reflection.put("statusTags", list(safeSignals.get("statusTags")));
                reflection.put("readabilityMode", string(safeSignals.getOrDefault("readabilityMode", "TECNICO_ESTRUTURADO")));
            }
            case "AGENDA", "CALENDARIO" -> {
                reflection.put("calendarLane", string(safeSignals.getOrDefault("calendarLane", "GERAL")));
                reflection.put("calendarFocus", string(safeSignals.getOrDefault("calendarFocus", "ROTINA_INSTITUCIONAL")));
                reflection.put("deadlineProfile", string(safeSignals.getOrDefault("deadlineProfile", "OPERACIONAL_GERAL")));
            }
            case "WORKBENCH", "OPERACIONAL", "PENDENCIAS", "LANDSCAPE" -> {
                reflection.put("deadlineProfile", string(safeSignals.getOrDefault("deadlineProfile", "OPERACIONAL_GERAL")));
                reflection.put("calculatorEnabled", bool(safeSignals.get("calculatorEnabled")));
                reflection.put("calculatorDomains", list(safeSignals.get("calculatorDomains")));
                reflection.put("suggestedNativeSections", list(safeSignals.get("suggestedNativeSections")));
            }
            default -> reflection.put("suggestedNativeSections", list(safeSignals.get("suggestedNativeSections")));
        }
        out.put("signalReflection", Map.copyOf(reflection));
        return Collections.unmodifiableMap(out);
    }

    public Map<String, Object> buildNativeComposition(String panelCode,
                                                      Map<String, Object> signals) {
        Map<String, Object> safeSignals = map(signals);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("panelCode", string(panelCode));
        out.put("sectionOrder", list(safeSignals.get("nativeSectionOrder")));
        out.put("highlightedSection", string(safeSignals.getOrDefault("highlightedNativeSection", "PAINEL")));
        out.put("compositionMode", string(safeSignals.getOrDefault("compositionMode", "BALANCED_FLOW")));
        out.put("collapseSecondaryBlocks", bool(safeSignals.get("collapseSecondaryBlocks")));
        out.put("listOrderingMode", string(safeSignals.getOrDefault("listOrderingMode", "BALANCED")));
        out.put("sectionPriorities", map(safeSignals.get("nativeSectionPriorities")));
        out.put("priorityTag", string(safeSignals.getOrDefault("priorityTag", "NORMAL")));
        out.put("attentionScore", intValue(safeSignals.get("attentionScore")));
        return Collections.unmodifiableMap(out);
    }

    private String resolveCompositionMode(int pendingCount, int urgentDeadlines) {
        if (urgentDeadlines > 0) {
            return "FOCUS_FLOW";
        }
        if (pendingCount >= 15) {
            return "PRIORITY_FLOW";
        }
        return "BALANCED_FLOW";
    }

    private boolean shouldCollapseSecondaryBlocks(int pendingCount, int urgentDeadlines) {
        return urgentDeadlines > 0 || pendingCount >= 25;
    }

    private Map<String, Integer> sectionPriorityMap(List<String> sectionOrder) {
        LinkedHashMap<String, Integer> out = new LinkedHashMap<>();
        int rank = 1;
        for (String section : sectionOrder) {
            out.put(string(section), rank++);
        }
        return Collections.unmodifiableMap(out);
    }

    private Integer resolveDisplayOrder(String blockName, Map<String, Object> signals) {
        Object value = map(signals.get("nativeSectionPriorities")).get(normalizeBlockToSection(blockName));
        return value instanceof Integer integer ? integer : 99;
    }

    private String resolveDisplayPriority(String blockName, Map<String, Object> signals) {
        return isHighlightedBlock(blockName, signals) ? "PRIMARY" : "SECONDARY";
    }

    private boolean isHighlightedBlock(String blockName, Map<String, Object> signals) {
        String highlighted = string(signals.getOrDefault("highlightedNativeSection", "PAINEL"));
        return normalizeBlockToSection(blockName).equals(highlighted);
    }

    private String normalizeBlockToSection(String blockName) {
        return switch (string(blockName)) {
            case "AGENDA", "CALENDARIO" -> "AGENDA";
            case "PENDENCIAS" -> "PRAZOS";
            case "WORKBENCH" -> "WORKBENCH_INVESTIGATIVO";
            case "LANDSCAPE" -> "TRIAGEM";
            case "VISUAL_IDENTITY", "BRANDING" -> "PAINEL";
            default -> string(blockName);
        };
    }

    private int attentionScore(int pendingCount, int urgentDeadlines) {
        return Math.min(100, Math.max(0, (pendingCount * 2) + (urgentDeadlines * 15)));
    }

    private String resolvePriorityTag(int pendingCount, int urgentDeadlines) {
        if (urgentDeadlines > 0) {
            return "CRITICO_24H";
        }
        if (pendingCount >= 15) {
            return "ATENCAO";
        }
        return "NORMAL";
    }

    private List<String> suggestedNativeSections(String panelCode) {
        return switch (string(panelCode)) {
            case "SECRETARIA" -> List.of("FILA", "PRAZO_RADAR", "AGENDA", "COMUNICACOES", "MIGRACAO");
            case "OFICIAL_JUSTICA" -> List.of("MANDADOS", "AGENDA_OPERACIONAL", "BALCAO_VIRTUAL", "RASTREIO");
            case "DELEGADO" -> List.of("INQUERITOS", "ALERTAS", "WORKBENCH_INVESTIGATIVO", "TRIAGEM");
            case "MINISTERIO_PUBLICO" -> List.of("MANIFESTACOES", "RECURSOS", "INQUERITOS", "PRAZOS");
            case "DEFENSOR_PUBLICO" -> List.of("ASSISTIDOS", "PETICOES", "RECURSOS", "AUDIENCIAS");
            case "DESEMBARGADOR_COLEGIADO" -> List.of("RELATORIA", "PAUTA", "SESSAO", "ACORDAO");
            default -> List.of("PAINEL", "PRAZOS", "AGENDA");
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> casted) {
            return (Map<String, Object>) casted;
        }
        return Map.of();
    }

    private List<String> list(Object value) {
        if (value instanceof List<?> casted) {
            return casted.stream().map(this::string).toList();
        }
        return List.of();
    }

    private boolean bool(Object value) {
        return value instanceof Boolean casted ? casted : Boolean.parseBoolean(string(value));
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(string(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
