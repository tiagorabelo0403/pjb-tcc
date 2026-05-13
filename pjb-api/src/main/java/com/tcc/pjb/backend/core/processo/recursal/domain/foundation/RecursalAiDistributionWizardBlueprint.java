package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalAiDistributionWizardBlueprint {

    private RecursalAiDistributionWizardBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                RecursalFormalSectionLabels.WIZARD_DISTRIBUICAO_AI_5_ETAPAS,
                RecursalFormalSectionLabels.INFERENCIA_ASSUNTO_CLASSE_COMPETENCIA,
                RecursalFormalSectionLabels.PREFLIGHT_CANONICAL_ROUTING
        );
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("ABRIR_WIZARD_GUIADO", "oferecer wizard externo de distribuição guiada para o recursal com etapas previsíveis de dados iniciais, assuntos, partes, características e documentos");
        checklist.put("INFERIR_INTENCAO_E_ASSUNTO", "usar inferência assistida para sugerir classe, assunto e competência a partir da narrativa e da peça submetida: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.ajuizamentoInferIntent(),
                RecursalWorkbenchSurfaceCatalog.ajuizamentoRouting(),
                RecursalWorkbenchSurfaceCatalog.ajuizamentoCapabilitiesTribunal())));
        checklist.put("RODAR_PREFLIGHT_E_CANONICAL", "antes da distribuição, fechar preflight, canonicalização e saneamento de rota para reduzir erro operacional: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.ajuizamentoPreflight(),
                RecursalWorkbenchSurfaceCatalog.ajuizamentoCanonical(),
                RecursalWorkbenchSurfaceCatalog.forumDistributionResolve())));
        checklist.put("PRESERVAR_ESCOLHA_ASSISTIDA_NAO_AUTONOMA", "a IA assiste a classificação e a competência, mas não pode distribuir autonomamente quando houver ambiguidade material ou dependência de processo de referência");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                "a distribuição assistida por IA deve permanecer explicável, com preflight e canonicalização antes do protocolo, sem atalho opaco para competência recursal",
                "quando houver ambiguidade de competência, rito ou órgão fracionário, a escolha continua assistida e auditável, nunca silenciosa"
        );
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "ativar wizard de distribuição assistida por IA para o recursal, usando inferência, preflight, canonicalização e resolução de competência sem substituir a validação humana em cenários ambíguos.";
    }
}
