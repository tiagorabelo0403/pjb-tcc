package com.tcc.pjb.backend.service.painel.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PainelExecutionSurfaceCompositionService {

    public Map<String, Object> buildExecutionSurface(String panelCode,
                                                     Map<String, Object> operationalSignals,
                                                     Map<String, Object> nativeComposition,
                                                     Map<String, Object> collectionComposition,
                                                     Map<String, Object> actionSurface) {
        Map<String, Object> safeOperationalSignals = map(operationalSignals);
        Map<String, Object> safeNativeComposition = map(nativeComposition);
        Map<String, Object> safeActionSurface = map(actionSurface);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        String normalizedPanel = normalize(panelCode);
        String priorityTag = string(safeOperationalSignals.getOrDefault("priorityTag", "NORMAL"));
        String highlightedSection = string(safeNativeComposition.getOrDefault("highlightedSection", "PAINEL"));
        String dominantActionLane = string(safeActionSurface.getOrDefault("dominantActionLane", "PAINEL"));
        out.put("panelCode", string(panelCode));
        out.put("priorityTag", priorityTag);
        out.put("attentionScore", intValue(safeOperationalSignals.get("attentionScore")));
        out.put("dominantContext", string(safeOperationalSignals.getOrDefault("dominantContext", "PAINEL")));
        out.put("highlightedSection", highlightedSection);
        out.put("executionMode", executionMode(priorityTag));
        out.put("commandGroupingMode", commandGroupingMode(priorityTag, normalizedPanel));
        out.put("dominantTransactionLane", dominantTransactionLane(normalizedPanel, dominantActionLane));
        out.put("primaryCta", primaryCta(normalizedPanel, priorityTag, highlightedSection));
        out.put("workflowStarter", workflowStarter(normalizedPanel, highlightedSection));
        out.put("standardTransactions", standardTransactions(normalizedPanel, priorityTag));
        out.put("quickCommands", quickCommands(normalizedPanel, highlightedSection));
        out.put("itemCommandPolicy", itemCommandPolicy(normalizedPanel, collectionComposition));
        return Collections.unmodifiableMap(out);
    }

    public Map<String, Object> decorateBlock(String panelCode,
                                             String blockName,
                                             Map<String, Object> block,
                                             Map<String, Object> executionSurface,
                                             Map<String, Object> nativeComposition) {
        Map<String, Object> safeExecutionSurface = map(executionSurface);
        Map<String, Object> safeNativeComposition = map(nativeComposition);
        LinkedHashMap<String, Object> out = block == null ? new LinkedHashMap<>() : new LinkedHashMap<>(block);
        LinkedHashMap<String, Object> hints = new LinkedHashMap<>();
        hints.put("highlighted", isHighlighted(blockName, safeNativeComposition));
        hints.put("executionMode", string(safeExecutionSurface.getOrDefault("executionMode", "STANDARD_EXECUTION")));
        hints.put("dominantTransactionLane", string(safeExecutionSurface.getOrDefault("dominantTransactionLane", "PAINEL")));
        hints.put("primaryCtaLabel", string(map(safeExecutionSurface.get("primaryCta")).getOrDefault("label", "Abrir fluxo")));
        hints.put("workflowStarterId", string(map(safeExecutionSurface.get("workflowStarter")).getOrDefault("code", "PAINEL_FLOW")));
        hints.put("quickCommandMode", string(safeExecutionSurface.getOrDefault("commandGroupingMode", "STANDARD_CLUSTER")));
        hints.put("promotedTransactions", promotedTransactions(blockName, safeExecutionSurface));
        hints.put("quickCommands", quickCommandsForBlock(blockName, safeExecutionSurface));
        out.put("executionSurfaceHints", Map.copyOf(hints));
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> primaryCta(String panelCode, String priorityTag, String highlightedSection) {
        if (panelCode.contains("SECRETARIA")) {
            return cta("EXPEDIR_INTIMACAO_CRITICA", "Expedir intimação crítica", "/api/v1/servidor/operacional/intimacao", priorityTag, highlightedSection);
        }
        if (panelCode.contains("OFICIAL")) {
            return cta("ABRIR_MANDADO_URGENTE", "Abrir mandado urgente", "/api/v1/oficial-justica/mandados", priorityTag, highlightedSection);
        }
        if (panelCode.contains("DELEGADO")) {
            return cta("TRIAR_INQUERITO", "Triar inquérito prioritário", "/api/v1/delegado/inqueritos/triagem", priorityTag, highlightedSection);
        }
        if (panelCode.contains("MINISTERIO_PUBLICO")) {
            return cta("LANCAR_MANIFESTACAO", "Lançar manifestação prioritária", "/api/v1/mp/manifestacoes", priorityTag, highlightedSection);
        }
        if (panelCode.contains("DEFENSOR_PUBLICO")) {
            return cta("REDIGIR_PETICAO", "Redigir petição prioritária", "/api/v1/defensoria/peticoes", priorityTag, highlightedSection);
        }
        if (panelCode.contains("DESEMBARGADOR") || panelCode.contains("COLEGIADO")) {
            return cta("PREPARAR_VOTO", "Preparar voto prioritário", "/api/v1/colegiado/votos", priorityTag, highlightedSection);
        }
        return cta("ABRIR_FLUXO", "Abrir fluxo prioritário", "/api/v1/painel", priorityTag, highlightedSection);
    }

    private Map<String, Object> workflowStarter(String panelCode, String highlightedSection) {
        if (panelCode.contains("SECRETARIA")) {
            return workflow("SECRETARIA_ATOS_FLOW", "Fluxo de atos cartorários", "/api/v1/secretariat/queue", highlightedSection);
        }
        if (panelCode.contains("OFICIAL")) {
            return workflow("CUMPRIMENTO_EXTERNO_FLOW", "Fluxo de cumprimento externo", "/api/v1/oficial-justica/workbench", highlightedSection);
        }
        if (panelCode.contains("DELEGADO")) {
            return workflow("TRIAGEM_INVESTIGATIVA_FLOW", "Fluxo de triagem investigativa", "/api/v1/delegado/inqueritos", highlightedSection);
        }
        if (panelCode.contains("MINISTERIO_PUBLICO")) {
            return workflow("ATUACAO_FINALISTICA_FLOW", "Fluxo finalístico do MP", "/api/v1/mp/processos", highlightedSection);
        }
        if (panelCode.contains("DEFENSOR_PUBLICO")) {
            return workflow("DEFENSORIA_OPERACIONAL_FLOW", "Fluxo operacional da defensoria", "/api/v1/defensoria/assistidos", highlightedSection);
        }
        if (panelCode.contains("DESEMBARGADOR") || panelCode.contains("COLEGIADO")) {
            return workflow("FLUXO_COLEGIADO", "Fluxo colegiado priorizado", "/api/v1/colegiado/pauta", highlightedSection);
        }
        return workflow("PAINEL_FLOW", "Fluxo principal do painel", "/api/v1/painel", highlightedSection);
    }

    private List<Map<String, Object>> standardTransactions(String panelCode, String priorityTag) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (panelCode.contains("SECRETARIA")) {
            items.add(tx("JUNTADA", "Registrar juntada", "/api/v1/servidor/operacional/juntada", priorityTag));
            items.add(tx("INTIMACAO", "Expedir intimação", "/api/v1/servidor/operacional/intimacao", priorityTag));
            items.add(tx("MANDADO", "Expedir mandado", "/api/v1/servidor/operacional/mandado", priorityTag));
        } else if (panelCode.contains("OFICIAL")) {
            items.add(tx("DILIGENCIA", "Registrar diligência", "/api/v1/oficial-justica/diligencias", priorityTag));
            items.add(tx("PENHORA", "Atualizar penhora", "/api/v1/oficial-justica/penhoras", priorityTag));
            items.add(tx("CIENCIA", "Confirmar ciência", "/api/v1/oficial-justica/notificacoes/ciencia", priorityTag));
        } else if (panelCode.contains("DELEGADO")) {
            items.add(tx("TRIAGEM", "Executar triagem", "/api/v1/delegado/inqueritos/triagem", priorityTag));
            items.add(tx("DILIGENCIA", "Registrar diligência policial", "/api/v1/delegado/diligencias", priorityTag));
            items.add(tx("ALERTA", "Abrir alerta investigativo", "/api/v1/delegado/alertas", priorityTag));
        } else if (panelCode.contains("MINISTERIO_PUBLICO")) {
            items.add(tx("MANIFESTACAO", "Lançar manifestação", "/api/v1/mp/manifestacoes", priorityTag));
            items.add(tx("RECURSO", "Preparar recurso", "/api/v1/mp/recursos", priorityTag));
        } else if (panelCode.contains("DEFENSOR_PUBLICO")) {
            items.add(tx("PETICAO", "Redigir petição", "/api/v1/defensoria/peticoes", priorityTag));
            items.add(tx("RECURSO", "Abrir recurso", "/api/v1/defensoria/recursos", priorityTag));
        } else if (panelCode.contains("DESEMBARGADOR") || panelCode.contains("COLEGIADO")) {
            items.add(tx("VOTO", "Preparar voto", "/api/v1/colegiado/votos", priorityTag));
            items.add(tx("ACORDAO", "Publicar acórdão", "/api/v1/secretariat/operacional/colegiado/julgamentos", priorityTag));
        }
        return List.copyOf(items);
    }

    private List<Map<String, Object>> quickCommands(String panelCode, String highlightedSection) {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(cmd("ABRIR_SECAO", "Abrir seção destacada", "/api/v1/painel/section", highlightedSection));
        items.add(cmd("ATUALIZAR_PRIORIDADE", "Atualizar prioridade", "/api/v1/painel/priority", highlightedSection));
        if (panelCode.contains("SECRETARIA")) {
            items.add(cmd("COMANDO_PROCESSO", "Abrir comando rápido por processo", "/api/v1/secretariat/queue/item/command", "PROCESSO"));
        } else if (panelCode.contains("OFICIAL")) {
            items.add(cmd("COMANDO_MANDADO", "Abrir comando rápido por mandado", "/api/v1/oficial-justica/mandados/comando", "MANDADO"));
        } else if (panelCode.contains("DELEGADO")) {
            items.add(cmd("COMANDO_INQUERITO", "Abrir comando rápido por inquérito", "/api/v1/delegado/inqueritos/comando", "INQUERITO"));
        } else if (panelCode.contains("MINISTERIO_PUBLICO")) {
            items.add(cmd("COMANDO_FEITO", "Abrir comando rápido por feito", "/api/v1/mp/processos/comando", "PROCESSO"));
        } else if (panelCode.contains("DEFENSOR_PUBLICO")) {
            items.add(cmd("COMANDO_ASSISTIDO", "Abrir comando rápido por assistido", "/api/v1/defensoria/assistidos/comando", "ASSISTIDO"));
        } else if (panelCode.contains("DESEMBARGADOR") || panelCode.contains("COLEGIADO")) {
            items.add(cmd("COMANDO_RECURSO", "Abrir comando rápido por recurso", "/api/v1/colegiado/recursos/comando", "RECURSO"));
        }
        return List.copyOf(items);
    }

    private Map<String, Object> itemCommandPolicy(String panelCode, Map<String, Object> collectionComposition) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> collections = map(map(collectionComposition).getOrDefault("collections", Map.of()));
        out.put("panelCode", string(panelCode));
        out.put("supportsItemCommands", true);
        out.put("collectionTargets", List.copyOf(collections.keySet().stream().map(this::string).toList()));
        out.put("defaultCommandType", panelCode.contains("OFICIAL") ? "MANDADO" : panelCode.contains("DELEGADO") ? "INQUERITO" : "PROCESSO");
        out.put("bulkCommandAllowed", !panelCode.contains("DESEMBARGADOR"));
        return Collections.unmodifiableMap(out);
    }

    private List<Map<String, Object>> promotedTransactions(String blockName, Map<String, Object> executionSurface) {
        List<Map<String, Object>> transactions = listOfMaps(executionSurface.get("standardTransactions"));
        String normalizedBlock = normalize(blockName);
        return transactions.stream()
                .filter(item -> normalizedBlock.isBlank() || normalize(string(item.get("targetHint"))).contains(normalizedBlock)
                        || normalizedBlock.contains(normalize(string(item.get("targetHint"))))
                        || normalizedBlock.contains("OPERACIONAL")
                        || normalizedBlock.contains("WORKBENCH"))
                .limit(2)
                .toList();
    }

    private List<Map<String, Object>> quickCommandsForBlock(String blockName, Map<String, Object> executionSurface) {
        return listOfMaps(executionSurface.get("quickCommands")).stream().limit(2).toList();
    }

    private String executionMode(String priorityTag) {
        return switch (priorityTag) {
            case "CRITICO_24H" -> "GUIDED_EXECUTION";
            case "ATENCAO" -> "ACCELERATED_EXECUTION";
            default -> "STANDARD_EXECUTION";
        };
    }

    private String commandGroupingMode(String priorityTag, String panelCode) {
        if ("CRITICO_24H".equals(priorityTag)) {
            return "CRITICAL_CLUSTER";
        }
        if (panelCode.contains("DELEGADO") || panelCode.contains("OFICIAL")) {
            return "TACTICAL_CLUSTER";
        }
        return "STANDARD_CLUSTER";
    }

    private String dominantTransactionLane(String panelCode, String dominantActionLane) {
        if (panelCode.contains("SECRETARIA")) return "ATOS_CARTORARIOS";
        if (panelCode.contains("OFICIAL")) return "CUMPRIMENTO_EXTERNO";
        if (panelCode.contains("DELEGADO")) return "TRIAGEM_INVESTIGATIVA";
        if (panelCode.contains("MINISTERIO_PUBLICO")) return "ATUACAO_FINALISTICA";
        if (panelCode.contains("DEFENSOR_PUBLICO")) return "ATUACAO_ASSISTENCIAL";
        if (panelCode.contains("DESEMBARGADOR") || panelCode.contains("COLEGIADO")) return "FLUXO_COLEGIADO";
        return dominantActionLane.isBlank() ? "PAINEL" : dominantActionLane;
    }

    private boolean isHighlighted(String blockName, Map<String, Object> nativeComposition) {
        return normalize(blockName).equals(normalize(string(nativeComposition.getOrDefault("highlightedSection", "PAINEL"))))
                || normalize(blockName).contains(normalize(string(nativeComposition.getOrDefault("highlightedSection", "PAINEL"))));
    }

    private Map<String, Object> cta(String code, String label, String path, String priorityTag, String targetHint) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("code", string(code));
        out.put("label", string(label));
        out.put("path", string(path));
        out.put("priorityBoost", "CRITICO_24H".equals(priorityTag));
        out.put("targetHint", string(targetHint));
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> workflow(String code, String label, String path, String targetHint) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("code", string(code));
        out.put("label", string(label));
        out.put("path", string(path));
        out.put("targetHint", string(targetHint));
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> tx(String code, String label, String path, String priorityTag) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("code", string(code));
        out.put("label", string(label));
        out.put("path", string(path));
        out.put("priorityBoost", "CRITICO_24H".equals(priorityTag));
        out.put("targetHint", string(code));
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> cmd(String code, String label, String path, String targetHint) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("code", string(code));
        out.put("label", string(label));
        out.put("path", string(path));
        out.put("targetHint", string(targetHint));
        return Collections.unmodifiableMap(out);
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
