package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalInstitutionalBoxesHistoryBlueprint {

    private RecursalInstitutionalBoxesHistoryBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                RecursalFormalSectionLabels.CAIXAS_RECURSAIS_ORGANIZACAO_E_FILTROS,
                RecursalFormalSectionLabels.HISTORICO_MOVIMENTACAO_DEVOLUCAO,
                RecursalFormalSectionLabels.VINCULO_REPRESENTANTES_E_COBERTURA
        );
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("ABRIR_CAIXAS_E_FILTROS_OPERACIONAIS", "reforçar caixas institucionais com filtros automáticos, vínculo funcional e visão recursal por papel: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.institutionalWorkbenchBoxes(),
                RecursalWorkbenchSurfaceCatalog.institutionalWorkbenchBoxFilters(),
                RecursalWorkbenchSurfaceCatalog.institutionalWorkbenchOperationalQueue())));
        checklist.put("PRESERVAR_HISTORICO_E_DEVOLUCAO", "manter histórico de movimentação, devolução e redistribuição da caixa institucional com trilha auditável: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.institutionalWorkbenchBoxHistory(),
                RecursalWorkbenchSurfaceCatalog.institutionalWorkbenchActionPreview())));
        checklist.put("SINCRONIZAR_REPRESENTANTES_E_COBERTURA", "sincronizar representantes processuais, cobertura e pré-pauta com a caixa institucional recursal: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.institutionalSupportCoverage(branchCode(request)),
                RecursalWorkbenchSurfaceCatalog.institutionalSupportPrePauta(branchCode(request)),
                RecursalWorkbenchSurfaceCatalog.institutionalWorkbenchQuickActions())));
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                "caixas institucionais recursais precisam de histórico de movimentação e devolução explícitos para não virarem fila opaca de gabinete paralelo",
                "a malha institucional deve distinguir gestor, distribuidor e representante processual sem romper cobertura, pré-pauta e quick actions já existentes"
        );
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "reforçar caixas institucionais recursais com filtros, histórico de movimentação, devolução auditável e sincronização de representantes, cobertura e pré-pauta no mesmo institutional workbench.";
    }

    private static String branchCode(RecursalAutomationRequest request) {
        String ramo = request.ramoProcessual() == null ? "" : request.ramoProcessual().trim().toUpperCase();
        return switch (ramo) {
            case "PENAL" -> "PROMOTORIA_PENAL";
            case "TRABALHISTA" -> "PROMOTORIA_TRABALHISTA";
            case "ELEITORAL" -> "PROMOTORIA_ELEITORAL";
            default -> "PROMOTORIA_CIVEL";
        };
    }
}
