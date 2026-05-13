package com.tcc.pjb.backend.service.painel.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PainelActionSurfaceCompositionService {

    public Map<String, Object> buildActionSurface(String panelCode,
                                                  Map<String, Object> operationalSignals,
                                                  Map<String, Object> nativeComposition,
                                                  Map<String, Object> collectionComposition) {
        Map<String, Object> safeOperationalSignals = map(operationalSignals);
        Map<String, Object> safeNativeComposition = map(nativeComposition);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        String normalizedPanel = normalize(panelCode);
        String priorityTag = string(safeOperationalSignals.getOrDefault("priorityTag", "NORMAL"));
        String highlightedSection = string(safeNativeComposition.getOrDefault("highlightedSection", "PAINEL"));
        out.put("panelCode", string(panelCode));
        out.put("priorityTag", priorityTag);
        out.put("attentionScore", intValue(safeOperationalSignals.get("attentionScore")));
        out.put("dominantContext", string(safeOperationalSignals.getOrDefault("dominantContext", "PAINEL")));
        out.put("highlightedSection", highlightedSection);
        out.put("menuMode", menuMode(priorityTag));
        out.put("dominantActionLane", dominantActionLane(normalizedPanel, highlightedSection));
        out.put("suggestedActions", suggestedActions(normalizedPanel, priorityTag, highlightedSection));
        out.put("primaryShortcuts", primaryShortcuts(normalizedPanel, highlightedSection));
        out.put("priorityCards", priorityCards(normalizedPanel, priorityTag, collectionComposition));
        out.put("contextualMenus", contextualMenus(normalizedPanel, highlightedSection));
        return Collections.unmodifiableMap(out);
    }

    public Map<String, Object> decorateBlock(String panelCode,
                                             String blockName,
                                             Map<String, Object> block,
                                             Map<String, Object> actionSurface,
                                             Map<String, Object> nativeComposition) {
        Map<String, Object> safeActionSurface = map(actionSurface);
        Map<String, Object> safeNativeComposition = map(nativeComposition);
        LinkedHashMap<String, Object> out = block == null ? new LinkedHashMap<>() : new LinkedHashMap<>(block);
        LinkedHashMap<String, Object> hints = new LinkedHashMap<>();
        hints.put("highlighted", isHighlighted(blockName, safeNativeComposition));
        hints.put("menuMode", string(safeActionSurface.getOrDefault("menuMode", "STANDARD_MENU")));
        hints.put("dominantActionLane", string(safeActionSurface.getOrDefault("dominantActionLane", "PAINEL")));
        hints.put("promotedActions", promotedActionsForBlock(blockName, safeActionSurface));
        hints.put("shortcutSuggestion", shortcutForBlock(blockName, safeActionSurface));
        hints.put("priorityCardSuggestion", priorityCardForBlock(blockName, safeActionSurface));
        out.put("actionSurfaceHints", Map.copyOf(hints));
        return Collections.unmodifiableMap(out);
    }

    private List<Map<String, Object>> suggestedActions(String panelCode, String priorityTag, String highlightedSection) {
        List<Map<String, Object>> actions = new ArrayList<>();
        if (panelCode.contains("SECRETARIA")) {
            actions.add(action("REALIZAR_JUNTADA", "Realizar juntada prioritária", "/api/v1/servidor/operacional/juntada", priorityTag));
            actions.add(action("EXPEDIR_INTIMACAO", "Expedir intimação crítica", "/api/v1/servidor/operacional/intimacao", priorityTag));
            actions.add(action("REORDENAR_CONCLUSOS", "Reordenar conclusos", "/api/v1/secretariat/queue", highlightedSection));
        } else if (panelCode.contains("OFICIAL")) {
            actions.add(action("ABRIR_MANDADO", "Abrir mandado urgente", "/api/v1/oficial-justica/mandados", priorityTag));
            actions.add(action("REGISTRAR_DILIGENCIA", "Registrar diligência", "/api/v1/oficial-justica/diligencias", highlightedSection));
            actions.add(action("ATUALIZAR_PENHORA", "Atualizar penhora", "/api/v1/oficial-justica/penhoras", priorityTag));
        } else if (panelCode.contains("DELEGADO")) {
            actions.add(action("TRIAR_INQUERITO", "Triar inquérito prioritário", "/api/v1/delegado/inqueritos/triagem", priorityTag));
            actions.add(action("REGISTRAR_DILIGENCIA", "Registrar diligência policial", "/api/v1/delegado/diligencias", highlightedSection));
            actions.add(action("ABRIR_ALERTA", "Abrir alerta investigativo", "/api/v1/delegado/alertas", priorityTag));
        } else if (panelCode.contains("MINISTERIO_PUBLICO")) {
            actions.add(action("LANCAR_MANIFESTACAO", "Lançar manifestação prioritária", "/api/v1/mp/manifestacoes", priorityTag));
            actions.add(action("PREPARAR_RECURSO", "Preparar recurso", "/api/v1/mp/recursos", highlightedSection));
        } else if (panelCode.contains("DEFENSOR_PUBLICO")) {
            actions.add(action("REDIGIR_PETICAO", "Redigir petição prioritária", "/api/v1/defensoria/peticoes", priorityTag));
            actions.add(action("REVISAR_AUDIENCIAS", "Revisar audiências", "/api/v1/defensoria/audiencias", highlightedSection));
        } else if (panelCode.contains("DESEMBARGADOR") || panelCode.contains("COLEGIADO")) {
            actions.add(action("PREPARAR_VOTO", "Preparar voto prioritário", "/api/v1/colegiado/votos", priorityTag));
            actions.add(action("PUBLICAR_ACORDAO", "Publicar acórdão", "/api/v1/secretariat/operacional/colegiado/julgamentos", highlightedSection));
            actions.add(action("ORGANIZAR_PAUTA", "Organizar pauta", "/api/v1/secretariat/operacional/colegiado/processos", priorityTag));
        } else {
            actions.add(action("ABRIR_PAINEL", "Abrir fluxo prioritário", "/api/v1/painel", priorityTag));
        }
        return List.copyOf(actions);
    }

    private List<Map<String, Object>> primaryShortcuts(String panelCode, String highlightedSection) {
        List<Map<String, Object>> shortcuts = new ArrayList<>();
        if (panelCode.contains("SECRETARIA")) {
            shortcuts.add(shortcut("Fila da secretaria", "/api/v1/secretariat/queue", highlightedSection));
            shortcuts.add(shortcut("Agenda institucional", "/api/v1/calendar/workspace", "AGENDA"));
        } else if (panelCode.contains("OFICIAL")) {
            shortcuts.add(shortcut("Mandados", "/api/v1/oficial-justica/mandados", highlightedSection));
            shortcuts.add(shortcut("Calendário operacional", "/api/v1/oficial-justica/calendario", "CALENDARIO"));
        } else if (panelCode.contains("DELEGADO")) {
            shortcuts.add(shortcut("Triagem investigativa", "/api/v1/delegado/inqueritos", highlightedSection));
            shortcuts.add(shortcut("Workbench multimídia", "/api/v1/delegado/multimidia", "WORKBENCH"));
        } else if (panelCode.contains("MINISTERIO_PUBLICO")) {
            shortcuts.add(shortcut("Processos prioritários", "/api/v1/mp/processos", highlightedSection));
            shortcuts.add(shortcut("Recursos", "/api/v1/mp/recursos", "RECURSOS"));
        } else if (panelCode.contains("DEFENSOR_PUBLICO")) {
            shortcuts.add(shortcut("Assistidos", "/api/v1/defensoria/assistidos", highlightedSection));
            shortcuts.add(shortcut("Petições", "/api/v1/defensoria/peticoes", "PETICOES"));
        } else if (panelCode.contains("DESEMBARGADOR") || panelCode.contains("COLEGIADO")) {
            shortcuts.add(shortcut("Pauta", "/api/v1/colegiado/pauta", highlightedSection));
            shortcuts.add(shortcut("Acórdãos", "/api/v1/colegiado/acordaos", "ACORDAO"));
        }
        return List.copyOf(shortcuts);
    }

    private List<Map<String, Object>> priorityCards(String panelCode,
                                                    String priorityTag,
                                                    Map<String, Object> collectionComposition) {
        List<Map<String, Object>> cards = new ArrayList<>();
        Map<String, Object> collections = map(map(collectionComposition).getOrDefault("collections", Map.of()));
        collections.forEach((name, meta) -> cards.add(priorityCard(string(name), meta, priorityTag)));
        if (cards.isEmpty()) {
            cards.add(priorityCard(panelCode + "_FLOW", Map.of("size", 0, "emptyState", "EMPTY"), priorityTag));
        }
        return List.copyOf(cards);
    }

    private List<Map<String, Object>> contextualMenus(String panelCode, String highlightedSection) {
        List<Map<String, Object>> menus = new ArrayList<>();
        menus.add(menu("Ações rápidas", List.of("Abrir fluxo prioritário", "Ver pendências", "Atualizar calendário")));
        if (panelCode.contains("SECRETARIA")) {
            menus.add(menu("Atos cartorários", List.of("Juntada", "Intimação", "Mandado", "Conclusão")));
        } else if (panelCode.contains("OFICIAL")) {
            menus.add(menu("Cumprimento externo", List.of("Mandado", "Penhora", "Balcão virtual", "Rastreio")));
        } else if (panelCode.contains("DELEGADO")) {
            menus.add(menu("Operação investigativa", List.of("Triagem", "BO", "Diligência", "Alertas")));
        } else if (panelCode.contains("MINISTERIO_PUBLICO")) {
            menus.add(menu("Atuação finalística", List.of("Manifestação", "Recurso", "Inquérito")));
        } else if (panelCode.contains("DEFENSOR_PUBLICO")) {
            menus.add(menu("Atuação assistencial", List.of("Assistido", "Petição", "Audiência")));
        } else if (panelCode.contains("DESEMBARGADOR") || panelCode.contains("COLEGIADO")) {
            menus.add(menu("Fluxo colegiado", List.of("Pauta", "Voto", "Acórdão", "Vista")));
        }
        menus.add(menu("Contexto dominante", List.of(highlightedSection.isBlank() ? "Painel" : highlightedSection)));
        return List.copyOf(menus);
    }

    private List<Map<String, Object>> promotedActionsForBlock(String blockName, Map<String, Object> actionSurface) {
        List<Map<String, Object>> actions = listOfMaps(actionSurface.get("suggestedActions"));
        if (actions.isEmpty()) {
            return List.of();
        }
        String normalizedBlock = normalize(blockName);
        return actions.stream()
                .filter(action -> normalizedBlock.contains(normalize(string(action.get("targetHint")))) || bool(action.get("priorityBoost")))
                .limit(2)
                .toList();
    }

    private Map<String, Object> shortcutForBlock(String blockName, Map<String, Object> actionSurface) {
        List<Map<String, Object>> shortcuts = listOfMaps(actionSurface.get("primaryShortcuts"));
        String normalizedBlock = normalize(blockName);
        return shortcuts.stream()
                .filter(shortcut -> normalizedBlock.contains(normalize(string(shortcut.get("targetHint")))))
                .findFirst()
                .orElse(shortcuts.isEmpty() ? Map.of() : shortcuts.getFirst());
    }

    private Map<String, Object> priorityCardForBlock(String blockName, Map<String, Object> actionSurface) {
        List<Map<String, Object>> cards = listOfMaps(actionSurface.get("priorityCards"));
        String normalizedBlock = normalize(blockName);
        return cards.stream()
                .filter(card -> normalizedBlock.contains(normalize(string(card.get("targetHint")))))
                .findFirst()
                .orElse(cards.isEmpty() ? Map.of() : cards.getFirst());
    }

    private boolean isHighlighted(String blockName, Map<String, Object> nativeComposition) {
        return normalize(blockName).contains(normalize(string(nativeComposition.getOrDefault("highlightedSection", "PAINEL"))));
    }

    private String menuMode(String priorityTag) {
        return priorityTag.contains("CRIT") ? "EMERGENCY_MENU" : priorityTag.contains("ATENCAO") ? "ATTENTION_MENU" : "STANDARD_MENU";
    }

    private String dominantActionLane(String panelCode, String highlightedSection) {
        if (panelCode.contains("SECRETARIA")) {
            return "ATOS_CARTORARIOS";
        }
        if (panelCode.contains("OFICIAL")) {
            return "CUMPRIMENTO_EXTERNO";
        }
        if (panelCode.contains("DELEGADO")) {
            return "TRIAGEM_INVESTIGATIVA";
        }
        if (panelCode.contains("MINISTERIO_PUBLICO")) {
            return "ATUACAO_FINALISTICA";
        }
        if (panelCode.contains("DEFENSOR_PUBLICO")) {
            return "ATUACAO_ASSISTENCIAL";
        }
        if (panelCode.contains("DESEMBARGADOR") || panelCode.contains("COLEGIADO")) {
            return "PAUTA_E_ACORDAO";
        }
        return highlightedSection.isBlank() ? "PAINEL" : highlightedSection;
    }

    private Map<String, Object> action(String code, String label, String path, String targetHint) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("code", string(code));
        out.put("label", string(label));
        out.put("path", string(path));
        out.put("targetHint", string(targetHint));
        out.put("priorityBoost", string(targetHint).contains("CRIT") || string(targetHint).contains("URG"));
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> shortcut(String label, String path, String targetHint) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("label", string(label));
        out.put("path", string(path));
        out.put("targetHint", string(targetHint));
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> priorityCard(String name, Object meta, String priorityTag) {
        Map<String, Object> safeMeta = map(meta);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("label", humanize(name));
        out.put("targetHint", string(name));
        out.put("priorityTag", string(priorityTag));
        out.put("size", intValue(safeMeta.get("size")));
        out.put("emptyState", string(safeMeta.getOrDefault("emptyState", intValue(safeMeta.get("size")) == 0 ? "EMPTY" : "READY")));
        out.put("highlighted", bool(safeMeta.get("highlighted")));
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> menu(String title, List<String> entries) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("title", string(title));
        out.put("entries", entries == null ? List.of() : List.copyOf(entries));
        return Collections.unmodifiableMap(out);
    }

    private String humanize(String raw) {
        return raw == null || raw.isBlank() ? "Item" : raw.replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> casted) {
            return (Map<String, Object>) casted;
        }
        return Map.of();
    }

    private List<Map<String, Object>> listOfMaps(Object value) {
        if (value instanceof List<?> casted) {
            return casted.stream().filter(Map.class::isInstance).map(Map.class::cast).map(this::map).toList();
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

    private String normalize(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
