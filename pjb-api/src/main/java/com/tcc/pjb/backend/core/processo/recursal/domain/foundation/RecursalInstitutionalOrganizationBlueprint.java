package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalInstitutionalOrganizationBlueprint {

    private RecursalInstitutionalOrganizationBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                RecursalFormalSectionLabels.REPRESENTANTE_PROCESSUAL_GESTOR_DISTRIBUIDOR_PADRAO,
                RecursalFormalSectionLabels.CAIXAS_ORGANIZACAO_FILTROS_HISTORICO,
                RecursalFormalSectionLabels.PROCURADORIA_DEFENSORIA_ENTIDADES_E_VINCULOS,
                RecursalFormalSectionLabels.PRE_PAUTA_COBERTURA_E_DISTRIBUICAO_INSTITUCIONAL
        );
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        String branchCode = branchCode(request);
        checklist.put("MODELAR_PAPEIS_REPRESENTACAO", "na representação institucional, separar papéis de gestor, distribuidor e representante padrão para que o recursal não concorra com a operação ordinária nem exponha permissões indevidas");
        checklist.put("ORGANIZAR_CAIXAS_E_FILTROS", "usar caixas de organização, filtros automáticos, devolução, vínculo de representantes e histórico de movimentação como malha de distribuição recursal institucional: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.institutionalWorkbench(),
                RecursalWorkbenchSurfaceCatalog.institutionalWorkbenchOperationalQueue(),
                RecursalWorkbenchSurfaceCatalog.institutionalWorkbenchQuickActions())));
        checklist.put("REUSAR_PROCURADORIA_E_DEFENSORIA", "reaproveitar dashboards executivo/organizacional de Procuradoria e Defensoria conectando entidades representadas, vínculos por processo e caixa operacional do ramo "
                + branchCode + ": "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.procuradoriaExecutiveDashboard(),
                RecursalWorkbenchSurfaceCatalog.procuradoriaOrganDashboard(),
                RecursalWorkbenchSurfaceCatalog.defensoriaExecutiveDashboard(),
                RecursalWorkbenchSurfaceCatalog.defensoriaOrganDashboard())));
        checklist.put("SINCRONIZAR_PRE_PAUTA_E_COBERTURA", "conectar agenda, cobertura, snapshot institucional e pré-pauta ao recurso ou embargo em curso, sem criar distribuição paralela fora do support/workbench: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.institutionalSupportSnapshot(branchCode),
                RecursalWorkbenchSurfaceCatalog.institutionalSupportAgenda(branchCode),
                RecursalWorkbenchSurfaceCatalog.institutionalSupportCoverage(branchCode),
                RecursalWorkbenchSurfaceCatalog.institutionalSupportPrePauta(branchCode))));
        checklist.put("LIMITAR_ENTIDADE_E_PARTE_CONFORME_ORGAO", "na Procuradoria, entidades representadas podem ser vinculadas ao órgão; na Defensoria, o vínculo nasce por processo/parte, e a lente recursal deve respeitar essa diferença estrutural");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        alertas.add("o institucional recursal não pode reduzir tudo a uma fila única; gestor, distribuidor e representante padrão precisam enxergar caixas e permissões compatíveis com o papel funcional");
        alertas.add("Procuradoria e Defensoria têm formas distintas de vincular representação, e a malha recursal deve respeitar isso antes de abrir cobertura, pré-pauta e fila operacional");
        if (request.perfilAtuacao() != null && !request.perfilAtuacao().isBlank()) {
            alertas.add("o perfil de atuação informado (" + request.perfilAtuacao().trim().toUpperCase() + ") deve modular quick actions e densidade de detalhe institucional");
        }
        return List.copyOf(alertas);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "orquestrar representação institucional recursal com papéis funcionais, caixas, filtros, cobertura, agenda e pré-pauta reaproveitando Procuradoria, Defensoria e institutional workbench já existentes.";
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
