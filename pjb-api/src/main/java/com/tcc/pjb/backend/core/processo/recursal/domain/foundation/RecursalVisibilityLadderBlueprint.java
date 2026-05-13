package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalVisibilityLadderBlueprint {

    private RecursalVisibilityLadderBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> sections = new ArrayList<>();
        sections.add(RecursalFormalSectionLabels.VISIBILIDADE_ESCALONADA_PARTES);
        sections.add(RecursalFormalSectionLabels.VISIBILIDADE_ESCALONADA_REPRESENTACAO_TECNICA);
        sections.add(RecursalFormalSectionLabels.VISIBILIDADE_ESCALONADA_APOIO_INSTITUCIONAL);
        sections.add(RecursalFormalSectionLabels.VISIBILIDADE_ESCALONADA_MAGISTRATURA_ORIGEM);
        sections.add(RecursalFormalSectionLabels.VISIBILIDADE_ESCALONADA_MAGISTRATURA_DESTINO);
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            sections.add(RecursalFormalSectionLabels.VISIBILIDADE_ESCALONADA_CORTE_SUPERIOR);
        }
        return List.copyOf(sections);
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("PUBLICAR_DEGRAU_PARTES", "fazer autor e réu enxergarem a fase recursal no painel externo já existente, com sigilo compatível: "
                + String.join(" | ", rotasPartesExternas()));
        checklist.put("PUBLICAR_DEGRAU_REPRESENTACAO", "depois do painel externo, refletir a mesma subida na representação técnica já existente de advogado ou escritório: "
                + String.join(" | ", rotasRepresentacaoTecnica()));
        checklist.put("PUBLICAR_DEGRAU_DEFENSORIA", "quando houver defesa pública, reutilizar o cockpit profissional da Defensoria sem painel paralelo: "
                + String.join(" | ", rotasDefensoria()));
        checklist.put("PUBLICAR_DEGRAU_APOIO_INSTITUCIONAL", "espelhar pré-pauta, cobertura e fila institucional no mesmo degrau operacional: "
                + String.join(" | ", rotasApoioInstitucional(request)));
        checklist.put("PUBLICAR_DEGRAU_MAGISTRATURA_ORIGEM", descricaoOrigem(recursoPrincipal, request));
        checklist.put("PUBLICAR_DEGRAU_MAGISTRATURA_DESTINO", descricaoDestino(recursoPrincipal, request));
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            checklist.put("PUBLICAR_DEGRAU_CORTE_SUPERIOR", "quando houver subida estrita, abrir o último degrau no painel e no colegiado da corte superior: "
                    + String.join(" | ", rotasCorteSuperior(recursoPrincipal, request)));
        }
        checklist.put("TRAVAR_SIGILO_POR_DEGRAU", "cada degrau deve respeitar credencial, sigilo e nível de detalhe: partes externas veem andamento compatível, representantes veem a malha recursal íntegra e magistratura recebe o cockpit decisório adequado");
        checklist.put("EVITAR_DUPLICACAO_DEGRAUS", "não criar painel novo para autor, réu, advogado, Defensoria ou magistratura se a superfície já existir em consulta pública, workspace profissional, institucional workbench, magistratura ou painéis colegiados; reutilizar no mesmo degrau "
                + RecursalWorkbenchSurfaceCatalog.publicConsultaWorkspace()
                + " | "
                + RecursalWorkbenchSurfaceCatalog.officeProcessAccess()
                + " | "
                + RecursalWorkbenchSurfaceCatalog.defensoriaExecutiveDashboard());
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        alertas.add("a subida recursal deve aparecer em degraus: parte externa, representação técnica, apoio institucional, magistratura de origem e magistratura de destino");
        alertas.add("o autor e o réu não precisam da caneta decisória, mas precisam enxergar a etapa recursal com linguagem e sigilo compatíveis até a última subida possível");
        if (!RecursalJurisdictionPanelBlueprint.mesmoOrgaoProlator(recursoPrincipal, request)) {
            alertas.add("quando subir, o órgão de origem preserva leitura, histórico e apoio; o órgão de destino passa a concentrar o julgamento e a pauta");
        }
        if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            alertas.add("no juizado a escada termina na turma recursal própria, sem conversão artificial para câmara clássica");
        }
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            alertas.add("na rota excepcional a escada ganha mais um degrau de presidência ou vice e, depois, corte superior");
        }
        return List.copyOf(alertas);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "organizar a visibilidade da subida recursal em degraus reaproveitando o que já existe para partes, representação técnica, apoio institucional e magistratura, sem painel paralelo e sem perder o vínculo entre origem e destino"
                + detalheDestino(recursoPrincipal, request)
                + ".";
    }

    private static List<String> rotasPartesExternas() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.publicConsultaWorkspace(),
                RecursalWorkbenchSurfaceCatalog.publicConsultaProcesso(),
                RecursalWorkbenchSurfaceCatalog.publicConsultaPageResolve()
        );
    }

    private static List<String> rotasRepresentacaoTecnica() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.officeProcessAccess(),
                RecursalWorkbenchSurfaceCatalog.officeProcessReadingMode(),
                RecursalWorkbenchSurfaceCatalog.officeWorkspaceMainDashboard(),
                RecursalWorkbenchSurfaceCatalog.officeWorkspaceExecutiveDashboard(),
                RecursalWorkbenchSurfaceCatalog.professionalWorkspaceExecutiveDashboard()
        );
    }

    private static List<String> rotasDefensoria() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.defensoriaExecutiveDashboard(),
                RecursalWorkbenchSurfaceCatalog.defensoriaOrganDashboard(),
                RecursalWorkbenchSurfaceCatalog.professionalWorkspaceOrganizationalDashboard()
        );
    }

    private static List<String> rotasApoioInstitucional(RecursalAutomationRequest request) {
        String branchCode = branchCode(request);
        return List.of(
                RecursalWorkbenchSurfaceCatalog.institutionalWorkbench(),
                RecursalWorkbenchSurfaceCatalog.institutionalWorkbenchOperationalQueue(),
                RecursalWorkbenchSurfaceCatalog.institutionalSupportPrePauta(branchCode),
                RecursalWorkbenchSurfaceCatalog.institutionalSupportCoverage(branchCode)
        );
    }

    private static List<String> rotasCorteSuperior(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.painelCorteSuperior(corteAxis(recursoPrincipal, request)),
                RecursalWorkbenchSurfaceCatalog.pautaCorteSuperior(0L),
                RecursalWorkbenchSurfaceCatalog.decisaoPlenariaCorteSuperior(0L)
        );
    }

    private static String descricaoOrigem(String recursoPrincipal, RecursalAutomationRequest request) {
        if (RecursalJurisdictionPanelBlueprint.mesmoOrgaoProlator(recursoPrincipal, request)) {
            return "como a atuação permanece no mesmo órgão prolator, manter o degrau da magistratura de origem no próprio painel decisório: "
                    + RecursalWorkbenchSurfaceCatalog.painelMagistradoPrimeiroGrau(justicaAxis(request), tribunalAxis(request));
        }
        return "no degrau da origem, preservar somente leitura, histórico, esclarecimentos e retorno operacional em "
                + RecursalWorkbenchSurfaceCatalog.magistratureExecutiveDashboard()
                + " e no painel do órgão prolator, sem manter a caneta decisória depois da subida";
    }

    private static String descricaoDestino(String recursoPrincipal, RecursalAutomationRequest request) {
        if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            return "no degrau de destino, publicar a rota recursal na turma recursal e no cockpit competente: "
                    + RecursalWorkbenchSurfaceCatalog.magistratureExecutiveDashboard()
                    + " | "
                    + RecursalWorkbenchSurfaceCatalog.painelColegiadoSegundoGrau(justicaAxis(request), "turma-recursal");
        }
        if (RecursalJurisdictionPanelBlueprint.mesmoOrgaoProlator(recursoPrincipal, request)) {
            return "o degrau de destino coincide com o próprio órgão prolator, mantendo painel e workbench já existentes sem redistribuição artificial";
        }
        return "no degrau de destino, abrir a atuação recursal no painel colegiado ou relatorial competente e no workbench de magistratura: "
                + RecursalWorkbenchSurfaceCatalog.magistratureExecutiveDashboard()
                + " | "
                + RecursalWorkbenchSurfaceCatalog.painelColegiadoSegundoGrau(justicaAxis(request), tribunalAxis(request))
                + " | "
                + RecursalWorkbenchSurfaceCatalog.magistraturaWorkspace();
    }

    private static String detalheDestino(String recursoPrincipal, RecursalAutomationRequest request) {
        if (RecursalJurisdictionPanelBlueprint.mesmoOrgaoProlator(recursoPrincipal, request)) {
            return ", mantendo a decisão no mesmo órgão prolator";
        }
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            return ", com degrau adicional de presidência ou vice e corte superior";
        }
        if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            return ", encerrando a subida na turma recursal própria";
        }
        return ", com transição do órgão de origem para gabinete relatorial e colegiado competentes";
    }

    private static String branchCode(RecursalAutomationRequest request) {
        return switch (segmentoAxis(request)) {
            case "FEDERAL" -> "branchCode-federal";
            case "ELEITORAL" -> "branchCode-eleitoral";
            case "MILITAR" -> "branchCode-militar";
            default -> "branchCode-estadual";
        };
    }

    private static String justicaAxis(RecursalAutomationRequest request) {
        String segmento = segmentoAxis(request);
        if (segmento.isBlank()) {
            return "estadual";
        }
        return segmento.toLowerCase();
    }

    private static String tribunalAxis(RecursalAutomationRequest request) {
        if (request.juizadoEspecial()) {
            return "turma-recursal";
        }
        return switch (segmentoAxis(request)) {
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
            case "RECURSO_ESPECIAL", "AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO", "EMBARGOS_DIVERGENCIA" -> "stj";
            default -> tribunalAxis(request);
        };
    }

    private static String segmentoAxis(RecursalAutomationRequest request) {
        return request.segmentoJudiciario() == null ? "" : request.segmentoJudiciario().trim().toUpperCase();
    }
}
