package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalRepresentationPanelBlueprint {

    private RecursalRepresentationPanelBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> sections = new ArrayList<>();
        sections.add(RecursalFormalSectionLabels.VISIBILIDADE_ESCALONADA_AUTOR_REU);
        sections.add(RecursalFormalSectionLabels.PAINEL_RECURSAL_ADVOCACIA_FILTRADO);
        sections.add(RecursalFormalSectionLabels.PAINEL_RECURSAL_DEFENSORIA_FILTRADO);
        sections.add(RecursalFormalSectionLabels.PAINEL_RECURSAL_PROCURADORIA_FILTRADO);
        sections.add(RecursalFormalSectionLabels.PAINEL_RECURSAL_MINISTERIO_PUBLICO_FILTRADO);
        sections.add(RecursalFormalSectionLabels.FILTRO_RITO_RECURSO_EMBARGOS);
        return List.copyOf(sections);
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("ESCALONAR_AUTOR_REU", "fazer autor e réu enxergarem a subida de forma organizada em degraus, primeiro no workspace público e depois na participação ativa quando houver autenticação: "
                + String.join(" | ", rotasAutorReu()));
        checklist.put("ABRIR_PAINEL_RECURSAL_ADVOCACIA", "abrir painel recursal dedicado da advocacia e do escritório, limitado a processos em recurso ou embargos e já filtrado por ramo/rito, classe recursal e polo processual: "
                + String.join(" | ", rotasAdvocacia()));
        checklist.put("ABRIR_PAINEL_RECURSAL_DEFENSORIA", "reaproveitar a lente executiva e organizacional da Defensoria com foco apenas em recurso e embargos, sem competir com o painel geral: "
                + String.join(" | ", rotasDefensoria()));
        checklist.put("ABRIR_PAINEL_RECURSAL_PROCURADORIA", "reaproveitar a lente executiva e organizacional da Procuradoria com o mesmo filtro recursal e conexão ao institutional workbench: "
                + String.join(" | ", rotasProcuradoria()));
        checklist.put("ABRIR_PAINEL_RECURSAL_MINISTERIO_PUBLICO", "quando houver Ministério Público no caso, abrir a visão recursal institucional conectada à pré-pauta, cobertura e workbench institucional: "
                + String.join(" | ", rotasMinisterioPublico(request)));
        checklist.put("TRAVAR_FILTROS_RECURSAIS", "o painel dedicado de representantes deve nascer já filtrado por " + filtroRecursal(recursoPrincipal, request));
        checklist.put("MOSTRAR_ULTIMAS_MOVIMENTACOES_RECURSAIS", "o painel recursal profissional deve destacar últimas movimentações, leitura orientada e avanço da trilha recursal sem sair das superfícies já existentes: "
                + String.join(" | ", rotasMovimentacaoRepresentacao()));
        checklist.put("REUSAR_CORES_PROCESSUAIS_EXISTENTES", "reaproveitar as cores processuais já criadas no reading-mode, no workspace executivo e na legenda oficial, sem paleta paralela: "
                + String.join(" | ", rotasCoresRepresentacao()));
        checklist.put("REUTILIZAR_SUPERFICIES_EXISTENTES", "não criar dashboard paralelo fora de office workspace, workspace profissional, painéis organizacionais e institutional workbench; o recursal deve entrar como lente dedicada dentro do que já existe");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        alertas.add("o cidadão precisa ver a escada do recurso em ordem: autor/réu -> representantes -> envolvidos institucionais -> órgão julgador competente");
        alertas.add("advogado, escritório, Defensoria, Procuradoria e Ministério Público não devem misturar acervo geral com acervo recursal; a lente dedicada deve abrir só processos em recurso ou embargos");
        alertas.add("a lente profissional precisa expor última movimentação relevante e a cor processual já existente para leitura rápida do degrau recursal");
        if (classificacaoRecursal(recursoPrincipal).equals("EMBARGOS")) {
            alertas.add("como a classe é de embargos, o painel dedicado deve destacar integração/correção e o órgão prolator competente sem forçar redistribuição artificial");
        } else {
            alertas.add("como a classe é recursal, o painel dedicado deve separar espécie recursal, órgão de destino e janela operacional da secretaria/relatoria");
        }
        if (request.recursoPrincipalInterposto()) {
            alertas.add("quando já houver recurso principal interposto, o degrau dos representantes deve exibir também a janela de contrarrazões e eventual adesivo compatível");
        }
        return List.copyOf(alertas);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "organizar a subida recursal para cidadão, advocacia, Defensoria, Procuradoria, Ministério Público e demais envolvidos em um degrau externo + um painel recursal profissional dedicado, reaproveitando os workspaces existentes e filtrando tudo por "
                + filtroRecursal(recursoPrincipal, request)
                + ".";
    }

    private static List<String> rotasAutorReu() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.publicConsultaWorkspace(),
                RecursalWorkbenchSurfaceCatalog.publicConsultaProcesso(),
                RecursalWorkbenchSurfaceCatalog.processualParticipacaoWorkspace(),
                RecursalWorkbenchSurfaceCatalog.processualParticipacaoSubmissoes()
        );
    }

    private static List<String> rotasAdvocacia() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.officeWorkspaceExecutiveDashboard(),
                RecursalWorkbenchSurfaceCatalog.officeProcessAccess(),
                RecursalWorkbenchSurfaceCatalog.officeProcessReadingMode(),
                RecursalWorkbenchSurfaceCatalog.professionalWorkspaceExecutiveDashboard()
        );
    }

    private static List<String> rotasDefensoria() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.defensoriaExecutiveDashboard(),
                RecursalWorkbenchSurfaceCatalog.defensoriaOrganDashboard(),
                RecursalWorkbenchSurfaceCatalog.professionalWorkspaceOrganizationalDashboard(),
                RecursalWorkbenchSurfaceCatalog.institutionalWorkbench()
        );
    }

    private static List<String> rotasProcuradoria() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.procuradoriaExecutiveDashboard(),
                RecursalWorkbenchSurfaceCatalog.procuradoriaOrganDashboard(),
                RecursalWorkbenchSurfaceCatalog.professionalWorkspaceOrganizationalDashboard(),
                RecursalWorkbenchSurfaceCatalog.institutionalWorkbench()
        );
    }

    private static List<String> rotasMinisterioPublico(RecursalAutomationRequest request) {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.professionalWorkspaceOrganizationalDashboard(),
                RecursalWorkbenchSurfaceCatalog.institutionalWorkbench(),
                RecursalWorkbenchSurfaceCatalog.institutionalSupportPrePauta(branchCode(request)),
                RecursalWorkbenchSurfaceCatalog.institutionalSupportCoverage(branchCode(request))
        );
    }


    private static List<String> rotasMovimentacaoRepresentacao() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.officeProcessReadingMode(),
                RecursalWorkbenchSurfaceCatalog.publicProcessTimeline(),
                RecursalWorkbenchSurfaceCatalog.processualParticipacaoWorkspace()
        );
    }

    private static List<String> rotasCoresRepresentacao() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.officeProcessReadingMode(),
                RecursalWorkbenchSurfaceCatalog.professionalWorkspaceExecutiveDashboard(),
                RecursalWorkbenchSurfaceCatalog.uiLegend()
        );
    }

    private static String filtroRecursal(String recursoPrincipal, RecursalAutomationRequest request) {
        String ramo = blank(request.ramoProcessual()) ? "RAMO_NAO_MAPEADO" : request.ramoProcessual().trim().toUpperCase();
        String rito = request.juizadoEspecial() ? "JUIZADO_ESPECIAL" : "RITO_ORDINARIO_DO_RAMO";
        String classe = classificacaoRecursal(recursoPrincipal);
        return "ramo=" + ramo + ", rito=" + rito + ", classe=" + classe + ", especie=" + recursoPrincipal;
    }

    private static String classificacaoRecursal(String recursoPrincipal) {
        return recursoPrincipal.startsWith("EMBARGOS") ? "EMBARGOS" : "RECURSO";
    }

    private static String branchCode(RecursalAutomationRequest request) {
        String ramo = blank(request.ramoProcessual()) ? "GERAL" : request.ramoProcessual().trim().toUpperCase();
        if (ramo.contains("PENAL")) {
            return "PROMOTORIA_PENAL";
        }
        if (ramo.contains("TRABALHISTA")) {
            return "PROMOTORIA_TRABALHISTA";
        }
        if (ramo.contains("ELEITORAL")) {
            return "PROMOTORIA_ELEITORAL";
        }
        return "PROMOTORIA_CIVEL";
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
