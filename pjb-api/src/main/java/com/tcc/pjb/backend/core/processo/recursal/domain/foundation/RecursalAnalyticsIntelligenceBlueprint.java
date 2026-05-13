package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalAnalyticsIntelligenceBlueprint {

    private RecursalAnalyticsIntelligenceBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                RecursalFormalSectionLabels.OBSERVABILIDADE_BI_RECUSAL,
                RecursalFormalSectionLabels.OBSERVABILIDADE_INDEXACAO_E_RECUPERACAO,
                RecursalFormalSectionLabels.OBSERVABILIDADE_NOTIFICA_MOBILE_E_ALERTAS
        );
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("ATIVAR_BI_RECURSAL", "ligar o workspace recursal a indicadores, produtividade e visão executiva sem sair da malha do processo ativo: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.recursalBusinessIntelligence(),
                RecursalWorkbenchSurfaceCatalog.professionalWorkspaceExecutiveDashboard(),
                RecursalWorkbenchSurfaceCatalog.processualPainelContextualTelemetria())));
        checklist.put("INDEXAR_E_RECUPERAR_INFORMACAO", "explicitar indexação, busca e recuperação inteligente de artefatos recursais e eventos relevantes: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.recursalIndexacaoBusca(),
                RecursalWorkbenchSurfaceCatalog.advogadoRelacaoProcessos(),
                RecursalWorkbenchSurfaceCatalog.processualPainelContextualRotaTatica())));
        checklist.put("CONECTAR_MOBILE_E_ALERTAS", "manter avisos pendentes, push, agenda e mobile recursal dentro da mesma governança multicanal do PJB: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.recursalNotificaPendencias(),
                RecursalWorkbenchSurfaceCatalog.recursalMobileAcompanhamento(),
                RecursalWorkbenchSurfaceCatalog.calendarNotificationPreview(),
                RecursalWorkbenchSurfaceCatalog.notificationPreferencesUser())));
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                "BI e indexação recursal precisam permanecer conectados ao processo ativo e não virar painel analítico órfão",
                "avisos móveis e pendências não podem introduzir nova malha de scheduler fora da governança central de notificações"
        );
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "orquestrar BI, indexação, recuperação de informação e avisos móveis recursais dentro da observabilidade e notificação já governadas pelo PJB.";
    }
}
