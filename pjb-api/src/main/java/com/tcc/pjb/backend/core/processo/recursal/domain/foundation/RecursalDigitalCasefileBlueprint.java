package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalDigitalCasefileBlueprint {

    private RecursalDigitalCasefileBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                RecursalFormalSectionLabels.AUTOS_DIGITAIS_CAPA_E_LEMBRETES,
                RecursalFormalSectionLabels.AUTOS_DIGITAIS_ASSUNTOS_PARTES_EVENTOS,
                RecursalFormalSectionLabels.AUTOS_DIGITAIS_ACOES_E_INFORMACOES_ADICIONAIS
        );
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("ABRIR_CAPA_LEMBRETES_E_RESUMO_OPERACIONAL", "publicar capa processual, lembretes, resumo recursal e radar de criticidade no mesmo autos digitais profissional: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.advogadoAutosDigitaisCapa(),
                RecursalWorkbenchSurfaceCatalog.advogadoAutosDigitaisLembretes(),
                RecursalWorkbenchSurfaceCatalog.personalProcessOverview(),
                RecursalWorkbenchSurfaceCatalog.processualPainelContextual())));
        checklist.put("EXIBIR_ASSUNTOS_PARTES_REPRESENTANTES_EVENTOS", "manter assuntos, partes, representantes, eventos e última movimentação estruturada sem ocultar o contexto recursal: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.advogadoAutosDigitaisAssuntos(),
                RecursalWorkbenchSurfaceCatalog.advogadoAutosDigitaisPartes(),
                RecursalWorkbenchSurfaceCatalog.advogadoAutosDigitaisEventos(),
                RecursalWorkbenchSurfaceCatalog.citizenEventMirror())));
        checklist.put("PRESERVAR_ACOES_E_INFORMACOES_ADICIONAIS", "conectar ações do processo, informações adicionais, histórico e atalhos do caso no mesmo shell profissional: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.advogadoAutosDigitaisInformacoesAdicionais(),
                RecursalWorkbenchSurfaceCatalog.advogadoAutosDigitaisAcoes(),
                RecursalWorkbenchSurfaceCatalog.professionalWorkspaceExecutiveDashboard(),
                RecursalWorkbenchSurfaceCatalog.processualPainelContextualRotaTatica())));
        checklist.put("RESTRICAO_A_ENVOLVIDOS", "o detalhamento dos autos digitais recursais só aprofunda dados sensíveis para envolvidos e representantes legitimados, preservando consulta neutra para quem não integra os autos");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                "autos digitais recursais não podem reduzir o processo a um download único sem capa, partes, eventos e ações operacionais explícitas",
                "o detalhamento do casefile deve reutilizar o workspace profissional já existente e não abrir viewer paralelo fora do eixo advogado/escritório"
        );
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "aprofundar os autos digitais recursais com capa, lembretes, assuntos, partes, informações adicionais, ações e eventos dentro do workspace profissional já existente.";
    }
}
