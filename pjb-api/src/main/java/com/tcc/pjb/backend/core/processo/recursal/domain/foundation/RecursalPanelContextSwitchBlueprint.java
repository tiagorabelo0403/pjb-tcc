package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalPanelContextSwitchBlueprint {

    private RecursalPanelContextSwitchBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                RecursalFormalSectionLabels.COMUTACAO_CONTEXTUAL_POR_RITO_TRIBUNAL,
                RecursalFormalSectionLabels.MODO_PROCESSO_ATIVO_NO_PAINEL,
                RecursalFormalSectionLabels.RETORNO_HOME_PADRAO_PJB,
                RecursalFormalSectionLabels.CONTEXTO_CIDADAO_POR_RITO_TRIBUNAL,
                RecursalFormalSectionLabels.CONTEXTO_REPRESENTACAO_TECNICA_POR_RITO_TRIBUNAL,
                RecursalFormalSectionLabels.CONTEXTO_SECRETARIA_POR_RITO_TRIBUNAL,
                RecursalFormalSectionLabels.CONTEXTO_MAGISTRATURA_POR_RITO_TRIBUNAL,
                RecursalFormalSectionLabels.CONTEXTO_INSTITUCIONAL_POR_RITO_TRIBUNAL
        );
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        String ramo = ramoAxis(request);
        String tribunal = tribunalAxis(request);
        checklist.put("COMUTAR_PROCESSO_ATIVO", "ao clicar em um processo do ramo " + ramo + ", comutar o shell do PJB para um modo contextual do processo ativo, com filtros, linguagem, prazos e atalhos do tribunal/órgão " + tribunal + ": " + String.join(" | ", shellContextSurfaces(ramo, request)));
        checklist.put("RESTAURAR_HOME_PADRAO", "ao sair do processo ou voltar à tela inicial, restaurar o PJB padrão sem deixar filtros, cor crítica, atalhos de rito ou workbench recursal contaminando o acervo geral");
        checklist.put("ADAPTAR_CIDADAO", "na visão do cidadão, abrir somente processos próprios com última movimentação, cor processual e leitura do ramo " + ramo + ": " + String.join(" | ", citizenContextSurfaces(ramo)));
        checklist.put("ADAPTAR_REPRESENTACAO", "na advocacia, Defensoria, Procuradoria e MP, abrir cockpit recursal do ramo " + ramo + " como lente própria do processo ativo, sem competir com o dashboard geral: " + String.join(" | ", representationContextSurfaces(ramo)));
        checklist.put("ADAPTAR_SECRETARIA", "a secretaria deve trocar queue, agenda, governance, coverage, catálogo formal e expedição conforme o rito/tribunal do processo ativo: " + String.join(" | ", secretariatContextSurfaces(ramo)));
        checklist.put("ADAPTAR_MAGISTRATURA", "a magistratura deve cair no workspace, preview e painel de órgão julgador compatíveis com o rito/tribunal do processo ativo: " + String.join(" | ", magistraturaContextSurfaces(request)));
        checklist.put("ADAPTAR_APOIO_INSTITUCIONAL", "o apoio institucional precisa abrir snapshot, agenda, pre-pauta e coverage coerentes com o ramo/branch do processo ativo: " + String.join(" | ", institutionalContextSurfaces(request)));
        checklist.put("TRAVAR_REGRAS_DO_RITO", "ao comutar o contexto, herdar a matriz de prazo, preparo/depósito, espécie recursal e filtros internos já descrita para ramo=" + ramo + ", tribunal=" + tribunal + ", classe=" + classificacaoRecursal(recursoPrincipal) + ", espécie=" + recursoPrincipal);
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        String ramo = ramoAxis(request);
        alertas.add("o PJB deve continuar sendo um sistema único, mas o processo ativo precisa abrir um shell contextual que faça o usuário sentir o rito " + ramo + " sem duplicar aplicação");
        alertas.add("ao abrir processo de outro ramo, os painéis de cidadão, representação, secretaria e magistratura devem mudar de comportamento, não só de filtro");
        alertas.add("ao voltar para a parte inicial, o sistema deve restaurar o modo padrão do PJB e remover contexto específico do processo anterior");
        if (ramo.equals("TRABALHISTA")) {
            alertas.add("no trabalhista, o cockpit deve priorizar execução, mídia, depósito e dinâmica TRT/TST sem herdar linguagem cível comum");
        }
        if (ramo.equals("PENAL")) {
            alertas.add("no penal, a comutação precisa reforçar sigilo, urgência e peças típicas de defesa/acusação em vez de reaproveitamento cível simplista");
        }
        if (ramo.equals("ELEITORAL") || ramo.equals("MILITAR")) {
            alertas.add("em " + ramo + ", o contexto precisa ficar mais comprimido e especializado porque prazos, órgãos e linguagem operacional são diferentes do eixo cível comum");
        }
        return List.copyOf(alertas);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        String ramo = ramoAxis(request);
        return "comutar o PJB para um contexto do processo ativo por rito, tribunal e perfil, de forma que abrir um processo do ramo "
                + ramo
                + " faça cidadão, representantes, secretaria, apoio institucional e magistratura trabalharem como se estivessem no subsistema daquele rito, mas retornando ao shell padrão ao sair do processo.";
    }

    private static List<String> shellContextSurfaces(String ramo, RecursalAutomationRequest request) {
        ArrayList<String> surfaces = new ArrayList<>();
        surfaces.add(RecursalWorkbenchSurfaceCatalog.uiLegend());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.calendarWorkspace());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.processoPrazoReal());
        surfaces.addAll(RecursalWorkbenchSurfaceCatalog.ramoSurfaceSummary(ramo).values());
        surfaces.addAll(RecursalWorkbenchSurfaceCatalog.familySummary(justicaAxis(request), tribunalAxis(request), corteAxis(request)).values());
        return List.copyOf(surfaces);
    }

    private static List<String> citizenContextSurfaces(String ramo) {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.citizenOwnProcesses(),
                RecursalWorkbenchSurfaceCatalog.citizenProcessOverview(),
                RecursalWorkbenchSurfaceCatalog.citizenTimelineVisual(),
                RecursalWorkbenchSurfaceCatalog.uiLegend()
        );
    }

    private static List<String> representationContextSurfaces(String ramo) {
        ArrayList<String> surfaces = new ArrayList<>();
        surfaces.add(RecursalWorkbenchSurfaceCatalog.officeWorkspaceExecutiveDashboard());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.professionalWorkspaceExecutiveDashboard());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.defensoriaExecutiveDashboard());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.procuradoriaExecutiveDashboard());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.ministerioPublicoPainel());
        surfaces.addAll(RecursalWorkbenchSurfaceCatalog.ramoSurfaceSummary(ramo).values());
        return List.copyOf(surfaces);
    }

    private static List<String> secretariatContextSurfaces(String ramo) {
        ArrayList<String> surfaces = new ArrayList<>();
        surfaces.add(RecursalWorkbenchSurfaceCatalog.secretariatQueuePanel());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.secretariatQueueAgenda());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.secretariatQueueGovernance());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.secretariatQueueCoverage());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.secretariatQueueFormalCatalog());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.secretariatOperationalSnapshot());
        surfaces.addAll(RecursalWorkbenchSurfaceCatalog.ramoSurfaceSummary(ramo).values());
        return List.copyOf(surfaces);
    }

    private static List<String> magistraturaContextSurfaces(RecursalAutomationRequest request) {
        ArrayList<String> surfaces = new ArrayList<>();
        surfaces.add(RecursalWorkbenchSurfaceCatalog.magistraturaWorkspace());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.magistraturaPreview());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.painelMagistradoPrimeiroGrau(justicaAxis(request), tribunalAxis(request)));
        surfaces.add(RecursalWorkbenchSurfaceCatalog.painelColegiadoSegundoGrau(justicaAxis(request), tribunalAxis(request)));
        surfaces.add(RecursalWorkbenchSurfaceCatalog.painelCorteSuperior(corteAxis(request)));
        return List.copyOf(surfaces);
    }

    private static List<String> institutionalContextSurfaces(RecursalAutomationRequest request) {
        String branch = branchCode(request);
        return List.of(
                RecursalWorkbenchSurfaceCatalog.institutionalWorkbench(),
                RecursalWorkbenchSurfaceCatalog.institutionalSupportSnapshot(branch),
                RecursalWorkbenchSurfaceCatalog.institutionalSupportAgenda(branch),
                RecursalWorkbenchSurfaceCatalog.institutionalSupportPrePauta(branch),
                RecursalWorkbenchSurfaceCatalog.institutionalSupportCoverage(branch)
        );
    }

    private static String ramoAxis(RecursalAutomationRequest request) {
        if (request.ramoProcessual() == null || request.ramoProcessual().isBlank()) {
            return "CIVEL";
        }
        return request.ramoProcessual().trim().toUpperCase();
    }

    private static String classificacaoRecursal(String recursoPrincipal) {
        return recursoPrincipal.startsWith("EMBARGOS") ? "EMBARGOS" : "RECURSO";
    }

    private static String justicaAxis(RecursalAutomationRequest request) {
        String segmento = request.segmentoJudiciario() == null ? "" : request.segmentoJudiciario().trim().toUpperCase();
        if (segmento.isBlank()) {
            return "estadual";
        }
        return switch (segmento) {
            case "FEDERAL" -> "federal";
            case "TRABALHISTA" -> "trabalhista";
            case "ELEITORAL" -> "eleitoral";
            case "MILITAR" -> "militar";
            default -> "estadual";
        };
    }

    private static String tribunalAxis(RecursalAutomationRequest request) {
        if (request.juizadoEspecial()) {
            return "turma-recursal";
        }
        return switch (justicaAxis(request)) {
            case "federal" -> "trf";
            case "trabalhista" -> "trt";
            case "eleitoral" -> "tre";
            case "militar" -> "tm";
            default -> "tj";
        };
    }

    private static String corteAxis(RecursalAutomationRequest request) {
        return switch (justicaAxis(request)) {
            case "federal" -> "stj-stf";
            case "trabalhista" -> "tst";
            case "eleitoral" -> "tse";
            case "militar" -> "stm";
            default -> "stj-stf";
        };
    }

    private static String branchCode(RecursalAutomationRequest request) {
        String ramo = ramoAxis(request);
        return switch (ramo) {
            case "PENAL" -> "PROMOTORIA_PENAL";
            case "TRABALHISTA" -> "PROMOTORIA_TRABALHISTA";
            case "ELEITORAL" -> "PROMOTORIA_ELEITORAL";
            case "MILITAR" -> "PROMOTORIA_MILITAR";
            default -> "PROMOTORIA_CIVEL";
        };
    }
}
