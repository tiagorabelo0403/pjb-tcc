package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalPanelRuntimeExperienceBlueprint {

    private RecursalPanelRuntimeExperienceBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                RecursalFormalSectionLabels.SHELL_CONTEXTUAL_RITO_TRIBUNAL,
                RecursalFormalSectionLabels.VOCABULARIO_OPERACIONAL_CONTEXTUAL,
                RecursalFormalSectionLabels.CARDS_PRIORIZADOS_CONTEXTO_ATIVO,
                RecursalFormalSectionLabels.ATALHOS_TATICOS_CONTEXTO_ATIVO,
                RecursalFormalSectionLabels.TELEMETRIA_E_FONTES_OFICIAIS_CONTEXTO_ATIVO,
                RecursalFormalSectionLabels.MODO_DETALHE_RAMO_TRIBUNAL,
                RecursalFormalSectionLabels.SILHUETA_VISUAL_SEM_RUIDO
        );
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        String ramo = ramoAxis(request);
        String tribunal = tribunalAxis(request);
        checklist.put("CARREGAR_SHELL_CONTEXTUAL", "ao abrir o processo ativo, carregar o shell contextual limpo do rito " + ramo + " no tribunal " + tribunal + ": " + String.join(" | ", shellSurfaces(request)));
        checklist.put("APLICAR_VOCABULARIO", "trocar microcopy, verbos, cards e prioridades para a linguagem operacional do rito " + ramo + ", sem contaminar a home padrão do PJB");
        checklist.put("PRIORIZAR_CARDS", "exibir cards táticos do contexto ativo com foco em prazo, movimento, rota e risco real: " + String.join(" | ", prioritizedCards(request)));
        checklist.put("ABRIR_ATALHOS_CERTOS", "ligar quick actions e atalhos táticos compatíveis com o ramo, o tribunal e o perfil autenticado: " + String.join(" | ", quickActions(request)));
        checklist.put("DETALHAR_FONTES_E_TELEMETRIA", "conectar telemetria, fontes oficiais e rota tática sem ruído visual: " + String.join(" | ", detailSurfaces(request)));
        checklist.put("PRESERVAR_SILHUETA_LIMPA", "reduzir ruído e poluição visual, deixando o painel com cards curtos, ações priorizadas e detalhe expandível apenas sob demanda");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        String ramo = ramoAxis(request);
        alertas.add("o shell contextual do processo ativo deve parecer outro subsistema do PJB, mas sem duplicar dashboard, controller ou contrato de superfície");
        alertas.add("o rito " + ramo + " precisa trocar vocabulário, cards e quick actions, não apenas filtros e legenda");
        if (ramo.equals("TRABALHISTA")) {
            alertas.add("no trabalhista, BNDT, execução, mídia e pendências operacionais devem subir para o topo do painel sem poluir a experiência geral");
        }
        if (ramo.equals("PENAL")) {
            alertas.add("no penal, habeas corpus, manifestação defensiva, parecer e urgência devem ganhar prioridade tática maior do que cards cíveis comuns");
        }
        if (ramo.equals("ELEITORAL") || ramo.equals("MILITAR")) {
            alertas.add("em " + ramo + ", o shell deve ficar mais comprimido, formal e enxuto por causa de prazos curtos, órgãos específicos e maior especialidade operacional");
        }
        if (ramo.equals("PREVIDENCIARIO")) {
            alertas.add("no previdenciário, o trilho especializado deve aparecer como detalhe tático do processo ativo e não como ruído estrutural fora do contexto");
        }
        return List.copyOf(alertas);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "ajustar o shell contextual do processo ativo para que o painel troque vocabulário, cards principais, atalhos táticos, telemetria e detalhe operacional conforme o rito, o tribunal e o perfil, mantendo a home do PJB limpa e sem duplicação.";
    }

    private static List<String> shellSurfaces(RecursalAutomationRequest request) {
        ArrayList<String> surfaces = new ArrayList<>();
        surfaces.add(RecursalWorkbenchSurfaceCatalog.processualPainelContextual());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.uiLegend());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.calendarWorkspace());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.processoPrazoReal());
        surfaces.addAll(detailSurfaces(request));
        return List.copyOf(surfaces);
    }

    private static List<String> prioritizedCards(RecursalAutomationRequest request) {
        ArrayList<String> cards = new ArrayList<>();
        cards.add(RecursalWorkbenchSurfaceCatalog.processoPrazoReal());
        cards.add(RecursalWorkbenchSurfaceCatalog.calendarPanel());
        cards.add(RecursalWorkbenchSurfaceCatalog.processualPainelContextualRotaTatica());
        cards.add(RecursalWorkbenchSurfaceCatalog.processualPendenciasPainel());
        String ramo = ramoAxis(request);
        if (ramo.equals("TRABALHISTA")) {
            cards.add(RecursalWorkbenchSurfaceCatalog.processualPainelContextualBndt());
        }
        if (ramo.equals("PREVIDENCIARIO")) {
            cards.add(RecursalWorkbenchSurfaceCatalog.processualPainelContextualTrilhoPrevidenciario());
        }
        if (ramo.equals("PENAL")) {
            cards.add(RecursalWorkbenchSurfaceCatalog.citizenJulgamentos());
        }
        return List.copyOf(cards);
    }

    private static List<String> quickActions(RecursalAutomationRequest request) {
        ArrayList<String> actions = new ArrayList<>();
        actions.add(RecursalWorkbenchSurfaceCatalog.processualParticipacaoProtocolar());
        actions.add(RecursalWorkbenchSurfaceCatalog.peticionamentoWizardProtocoloSimples());
        actions.add(RecursalWorkbenchSurfaceCatalog.magistraturaPreview());
        actions.add(RecursalWorkbenchSurfaceCatalog.institutionalWorkbenchQuickActions());
        String ramo = ramoAxis(request);
        switch (ramo) {
            case "PENAL" -> {
                actions.add(RecursalWorkbenchSurfaceCatalog.defensoriaHabeasCorpus());
                actions.add(RecursalWorkbenchSurfaceCatalog.ministerioPublicoParecer());
            }
            case "TRABALHISTA" -> {
                actions.add(RecursalWorkbenchSurfaceCatalog.secretariatOperationalLabourExecucao());
                actions.add(RecursalWorkbenchSurfaceCatalog.procuradoriaRecurso());
            }
            case "ELEITORAL" -> actions.add(RecursalWorkbenchSurfaceCatalog.secretariatOperationalElectoralCorregedoria());
            case "MILITAR" -> actions.add(RecursalWorkbenchSurfaceCatalog.secretariatOperationalMilitaryPlantao());
            default -> actions.add(RecursalWorkbenchSurfaceCatalog.ministerioPublicoManifestacao());
        }
        return List.copyOf(actions);
    }

    private static List<String> detailSurfaces(RecursalAutomationRequest request) {
        ArrayList<String> surfaces = new ArrayList<>();
        surfaces.add(RecursalWorkbenchSurfaceCatalog.processualPainelContextualTelemetria());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.processualPainelContextualFontesOficiais());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.processualPainelContextualRotaTatica());
        String ramo = ramoAxis(request);
        if (ramo.equals("TRABALHISTA")) {
            surfaces.add(RecursalWorkbenchSurfaceCatalog.processualPainelContextualBndt());
        }
        if (ramo.equals("PREVIDENCIARIO")) {
            surfaces.add(RecursalWorkbenchSurfaceCatalog.processualPainelContextualTrilhoPrevidenciario());
        }
        return List.copyOf(surfaces);
    }

    private static String ramoAxis(RecursalAutomationRequest request) {
        if (request.ramoProcessual() == null || request.ramoProcessual().isBlank()) {
            return "CIVEL";
        }
        return request.ramoProcessual().trim().toUpperCase();
    }

    private static String tribunalAxis(RecursalAutomationRequest request) {
        if (request.juizadoEspecial()) {
            return "turma-recursal";
        }
        String segmento = request.segmentoJudiciario() == null ? "" : request.segmentoJudiciario().trim().toUpperCase();
        return switch (segmento) {
            case "FEDERAL" -> "trf";
            case "TRABALHISTA" -> "trt";
            case "ELEITORAL" -> "tre";
            case "MILITAR" -> "tm";
            default -> "tj";
        };
    }
}
