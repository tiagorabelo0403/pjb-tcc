package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalOfficeCollaborationBlueprint {

    private RecursalOfficeCollaborationBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                RecursalFormalSectionLabels.ESCRITORIO_RECURSAL_EQUIPES_ASSISTENTES,
                RecursalFormalSectionLabels.SUBSTABELECIMENTO_RECURSAL_COM_E_SEM_RESERVA,
                RecursalFormalSectionLabels.SUBSTABELECIMENTO_RECURSAL_CANCELAMENTO_AUDITAVEL
        );
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("CONFIGURAR_ESCRITORIO_ASSISTENTES_E_EQUIPE", "explicitar escritório, equipes, assistentes e área de colaboração do advogado no mesmo cockpit profissional: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.advogadoEscritorio(),
                RecursalWorkbenchSurfaceCatalog.advogadoAssistentes(),
                RecursalWorkbenchSurfaceCatalog.advogadoAreaTrabalho(),
                RecursalWorkbenchSurfaceCatalog.professionalWorkspaceOrganizationalDashboard())));
        checklist.put("ORQUESTRAR_SUBSTABELECIMENTO", "suportar substabelecimento com e sem reserva, recebimento, cancelamento e espelho de cadeia representativa: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.advogadoSubstabelecimento(),
                RecursalWorkbenchSurfaceCatalog.advogadoSubstabelecimentoCancelamento(),
                RecursalWorkbenchSurfaceCatalog.advogadoRelacaoProcessos())));
        checklist.put("SINCRONIZAR_RESERVA_ASSINATURA_E_VISIBILIDADE", "preservar os limites de atuação do escritório e a cadeia de representação sem confundir assistente, substabelecido e patrono principal");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                "assistentes e substabelecidos não podem ser tratados como a mesma figura funcional no recursal",
                "cancelamento de substabelecimento exige trilha auditável e não pode apagar o histórico representativo do processo"
        );
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "aprofundar escritório, assistentes e substabelecimento recursal com reserva, cancelamento e cadeia auditável de representação no workspace profissional.";
    }
}
