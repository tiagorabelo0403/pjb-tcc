package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalAttorneyAssociationBlueprint {

    private RecursalAttorneyAssociationBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                RecursalFormalSectionLabels.HABILITACAO_ASSOCIACAO_PUBLICA_RECURSAL,
                RecursalFormalSectionLabels.HABILITACAO_ASSOCIACAO_SIGILOSA_RECURSAL,
                RecursalFormalSectionLabels.HABILITACAO_ASSOCIACAO_FLUXO_AUDITAVEL
        );
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("ASSOCIAR_PROCESSOS_PUBLICOS", "permitir associação e habilitação em processos públicos com trilha recursal explícita e sem bypass do vínculo profissional: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.advogadoAssociacaoProcessosPublicos(),
                RecursalWorkbenchSurfaceCatalog.advogadoSolicitacaoHabilitacao(),
                RecursalWorkbenchSurfaceCatalog.advogadoRelacaoProcessos())));
        checklist.put("SOLICITAR_HABILITACAO_SIGILOSA", "tratar processos sigilosos com solicitação governada, justificativa e auditoria antes de liberar visibilidade ampliada: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.advogadoHabilitacaoSigilosa(),
                RecursalWorkbenchSurfaceCatalog.notificationTrackingCiencia(),
                RecursalWorkbenchSurfaceCatalog.professionalWorkspaceOrganizationalDashboard())));
        checklist.put("TRAVAR_FLUXO_AUDITAVEL", "registrar deferimento, indeferimento, vinculação e revogação da habilitação de forma auditável sem criar canal paralelo fora do workspace profissional");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                "habilitação recursal em processo sigiloso não pode virar associação cega sem justificativa, deferimento e trilha auditável",
                "associação em processo público deve respeitar a fronteira entre consulta neutra e ingresso efetivo do representante nos autos"
        );
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "orquestrar associação e habilitação recursal de advogado em processos públicos e sigilosos com fluxo auditável e governado dentro do workspace profissional.";
    }
}
