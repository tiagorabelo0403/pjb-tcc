package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RecursalAdjudicationWorkbenchBlueprint {

    private RecursalAdjudicationWorkbenchBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> sections = new ArrayList<>();
        sections.add(RecursalFormalSectionLabels.PAINEL_MAGISTRADO_DESTINO);
        sections.add(RecursalFormalSectionLabels.WORKBENCH_MAGISTRATURA_DESTINO);
        if (!RecursalJurisdictionPanelBlueprint.mesmoOrgaoProlator(recursoPrincipal, request)) {
            sections.add(RecursalFormalSectionLabels.WORKBENCH_DISTRIBUICAO_DESTINO);
        }
        sections.add(RecursalFormalSectionLabels.WORKBENCH_SECRETARIA_OPERACIONAL_DESTINO);
        sections.add(RecursalFormalSectionLabels.WORKBENCH_INSTITUCIONAL_DESTINO);
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            sections.add(RecursalFormalSectionLabels.PAINEL_CORTE_SUPERIOR_DESTINO);
        }
        return List.copyOf(sections);
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("PLUGAR_PAINEL_MAGISTRADO", "ligar o handoff recursal ao painel real do órgão julgador competente: " + painelMagistradoDestino(recursoPrincipal, request));
        checklist.put("PLUGAR_WORKBENCH_MAGISTRATURA", "reusar o workspace e previews já existentes da magistratura em vez de novo cockpit satélite: "
                + String.join(" | ", rotasWorkbenchMagistratura(recursoPrincipal, request)));
        if (!RecursalJurisdictionPanelBlueprint.mesmoOrgaoProlator(recursoPrincipal, request)) {
            checklist.put("PLUGAR_WORKBENCH_DISTRIBUICAO", "apoiar prevenção, distribuição e relatoria no workbench real de distribuição: "
                    + RecursalWorkbenchSurfaceCatalog.distribuicaoWorkbench());
        }
        checklist.put("PLUGAR_SECRETARIA_OPERACIONAL", "fazer o órgão destino reaproveitar a malha operacional já existente de secretaria, julgamento e colegiado: "
                + String.join(" | ", rotasSecretariaDestino(recursoPrincipal, request)));
        checklist.put("PLUGAR_WORKBENCH_INSTITUCIONAL", "conectar MP, Defensoria e Procuradoria ao institutional workbench já existente: "
                + String.join(" | ", rotasWorkbenchInstitucional()));
        checklist.put("SELECIONAR_FAMILIA_POR_ORGAO", descricaoFamiliaPorOrgao(recursoPrincipal, request));
        checklist.put("EVITAR_DUPLICACAO_SUPERFICIE", "antes de criar tela ou endpoint novo, reaproveitar painel do magistrado, workbench da magistratura, distribuição processual, secretaria operacional e institutional workbench");
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            checklist.put("PLUGAR_CORTE_SUPERIOR", "quando houver rota excepcional, conectar também a borda de corte superior: "
                    + String.join(" | ", rotasCorteSuperior(recursoPrincipal, request)));
        }
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        alertas.add("o handoff recursal só fecha de verdade quando o processo aparece no painel e no workbench corretos do órgão competente");
        alertas.add("não abrir outro dashboard recursal genérico: reutilizar /api/v1/magistratura/atos, /api/v1/distribuicao/processual/workbench, secretaria operacional e /api/v1/institucional/workbench");
        if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            alertas.add("no juizado o destino continua colegiado próprio, com malha recursal própria e sem conversão artificial para câmara clássica");
        }
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            alertas.add("a presidência ou vice filtra a rota excepcional, mas a eventual subida precisa reaparecer também no painel e nos pontos operacionais da corte superior");
        }
        return List.copyOf(alertas);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "conectar o handoff recursal aos painéis e workbenches reais já existentes no PJB, reaproveitando magistratura, distribuição, secretaria operacional e institutional workbench para o órgão "
                + familiaOrgaoAlvo(recursoPrincipal, request)
                + ", sem cockpit paralelo nem duplicação de superfície.";
    }

    private static List<String> rotasWorkbenchMagistratura(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> routes = new ArrayList<>();
        routes.add(RecursalWorkbenchSurfaceCatalog.magistraturaWorkspace());
        routes.add(RecursalWorkbenchSurfaceCatalog.magistraturaPreview());
        routes.add(RecursalWorkbenchSurfaceCatalog.magistraturaAutomationPreview());
        if (!RecursalJurisdictionPanelBlueprint.mesmoOrgaoProlator(recursoPrincipal, request)) {
            routes.add(RecursalWorkbenchSurfaceCatalog.distribuicaoWorkbench());
        }
        return List.copyOf(routes);
    }

    private static List<String> rotasWorkbenchInstitucional() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.institutionalWorkbench(),
                RecursalWorkbenchSurfaceCatalog.institutionalWorkbenchQuickActions(),
                RecursalWorkbenchSurfaceCatalog.institutionalWorkbenchOperationalQueue(),
                RecursalWorkbenchSurfaceCatalog.institutionalWorkbenchActionPreview()
        );
    }

    private static List<String> rotasSecretariaDestino(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> routes = new ArrayList<>();
        if (RecursalJurisdictionPanelBlueprint.mesmoOrgaoProlator(recursoPrincipal, request)) {
            routes.add(OperationalApiRoutes.secretariatOperationalSnapshot());
            routes.add(OperationalApiRoutes.secretariatOperationalIntimacao(0L));
            routes.add(OperationalApiRoutes.secretariatOperationalConclusao(0L));
            return List.copyOf(routes);
        }
        routes.add(OperationalApiRoutes.secretariatQueuePanel());
        routes.add(OperationalApiRoutes.secretariatQueueAgenda());
        routes.add(OperationalApiRoutes.secretariatOperationalSnapshot());
        routes.add(OperationalApiRoutes.secretariatOperationalCollegiatePauta(0L));
        routes.add(OperationalApiRoutes.secretariatOperationalCollegiatePublication(0L));
        if (request.desejaSustentacaoOral()) {
            routes.add(OperationalApiRoutes.secretariatOperationalCollegiateSustentacao(0L));
        }
        routes.add(OperationalApiRoutes.secretariatOperationalCollegiateAcordao(0L));
        routes.add(OperationalApiRoutes.secretariatOperationalCollegiateBaixa(0L));
        return List.copyOf(routes);
    }

    private static List<String> rotasCorteSuperior(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> routes = new ArrayList<>();
        routes.add(RecursalWorkbenchSurfaceCatalog.painelCorteSuperior(corteAxis(recursoPrincipal, request)));
        routes.add(RecursalWorkbenchSurfaceCatalog.pautaCorteSuperior(0L));
        routes.add(RecursalWorkbenchSurfaceCatalog.decisaoPlenariaCorteSuperior(0L));
        routes.add(RecursalWorkbenchSurfaceCatalog.institutionalWorkbenchActionPreview());
        return List.copyOf(routes);
    }

    private static String painelMagistradoDestino(String recursoPrincipal, RecursalAutomationRequest request) {
        if (RecursalJurisdictionPanelBlueprint.mesmoOrgaoProlator(recursoPrincipal, request)) {
            return RecursalWorkbenchSurfaceCatalog.painelMagistradoPrimeiroGrau(justicaAxis(request), tribunalAxis(request));
        }
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            return RecursalWorkbenchSurfaceCatalog.painelColegiadoSegundoGrau(justicaAxis(request), tribunalAxis(request));
        }
        if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            return RecursalWorkbenchSurfaceCatalog.painelColegiadoSegundoGrau(justicaAxis(request), "turma-recursal");
        }
        return RecursalWorkbenchSurfaceCatalog.painelColegiadoSegundoGrau(justicaAxis(request), tribunalAxis(request));
    }

    private static String descricaoFamiliaPorOrgao(String recursoPrincipal, RecursalAutomationRequest request) {
        String family = familiaOrgaoAlvo(recursoPrincipal, request);
        Map<String, String> summary = RecursalWorkbenchSurfaceCatalog.familySummary(justicaAxis(request), tribunalAxis(request), corteAxis(recursoPrincipal, request));
        return "organizar o handoff pela família " + family + " e reaproveitar as superfícies reais: " + String.join(" | ", summary.values());
    }

    private static String familiaOrgaoAlvo(String recursoPrincipal, RecursalAutomationRequest request) {
        if (RecursalJurisdictionPanelBlueprint.mesmoOrgaoProlator(recursoPrincipal, request)) {
            return "órgão prolator original";
        }
        if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            return "turma recursal";
        }
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            return "presidência-vice e corte superior";
        }
        return "gabinete relatorial e colegiado recursal";
    }

    private static String justicaAxis(RecursalAutomationRequest request) {
        return switch (normalize(request.segmentoJudiciario())) {
            case "FEDERAL" -> "federal";
            case "TRABALHISTA" -> "trabalho";
            case "ELEITORAL" -> "eleitoral";
            case "MILITAR" -> "militar";
            default -> "estadual";
        };
    }

    private static String tribunalAxis(RecursalAutomationRequest request) {
        return switch (normalize(request.segmentoJudiciario())) {
            case "FEDERAL" -> "trf";
            case "TRABALHISTA" -> "trt";
            case "ELEITORAL" -> "tre";
            case "MILITAR" -> "tm";
            default -> "tj";
        };
    }

    private static String corteAxis(String recursoPrincipal, RecursalAutomationRequest request) {
        return switch (recursoPrincipal) {
            case "RECURSO_EXTRAORDINARIO" -> "stf";
            case "RECURSO_ESPECIAL", "AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO", "EMBARGOS_DIVERGENCIA" -> normalize(request.segmentoJudiciario()).equals("TRABALHISTA") ? "tst" : "stj";
            default -> "stj";
        };
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
