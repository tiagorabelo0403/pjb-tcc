package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalActorRiteTribunalPolicyBlueprint {

    private RecursalActorRiteTribunalPolicyBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                RecursalFormalSectionLabels.POLITICA_VISUAL_OPERACIONAL_ATOR_RITO_TRIBUNAL,
                RecursalFormalSectionLabels.CRITICIDADE_VISUAL_E_SEMAFORO_CONTEXTUAL,
                RecursalFormalSectionLabels.VOCABULARIO_E_TOM_INSTITUCIONAL,
                RecursalFormalSectionLabels.CARDS_E_ATALHOS_PRIMARIOS_CONTEXTO,
                RecursalFormalSectionLabels.DETALHE_EXPANDIVEL_E_DENSIDADE,
                RecursalFormalSectionLabels.POLITICA_RETORNO_HOME_E_ISOLAMENTO_CONTEXTO
        );
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        String perfil = perfilAtuacao(request);
        String ramo = ramoAxis(request);
        String tribunal = tribunalAxis(request);
        checklist.put("RESOLVER_MATRIZ_FINA", "resolver a matriz fina por ator, rito e tribunal antes de abrir o cockpit do processo ativo: perfil=" + perfil + ", ramo=" + ramo + ", tribunal=" + tribunal + ", recurso=" + recursoPrincipal);
        checklist.put("APLICAR_SEMAFORO_CONTEXTUAL", "aplicar criticidade visual, semáforo e densidade operacional compatíveis com o contexto ativo, preservando as cores processuais já existentes: " + criticidade(request));
        checklist.put("TROCAR_TOM_E_VOCABULARIO", "trocar linguagem e tom institucional para o perfil " + perfil + " no rito " + ramo + " dentro do tribunal " + tribunal + ", sem copiar microcopy do CPC comum para todos os painéis");
        checklist.put("PRIORIZAR_CARDS_E_ATALHOS", "priorizar cards e atalhos primários do processo ativo sem painel satélite: " + String.join(" | ", cardsPrimarios(request)) + " || " + String.join(" | ", atalhosPrimarios(request)));
        checklist.put("REGULAR_DENSIDADE_E_DETALHE", "regular detalhe expandível, densidade e profundidade do shell para evitar poluição e preservar foco operacional: " + densidade(request));
        checklist.put("ISOLAR_E_RESTAURAR_HOME", "isolar o shell do processo ativo e restaurar a home padrão do PJB ao sair do caso, sem vazar cards, semáforos ou atalhos de um rito para outro");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        String perfil = perfilAtuacao(request);
        String ramo = ramoAxis(request);
        String tribunal = tribunalAxis(request);
        alertas.add("o mesmo processo deve parecer um subsistema específico do PJB quando aberto, mas o sistema precisa voltar limpo para a home geral ao sair do contexto");
        alertas.add("perfil " + perfil + ", ramo " + ramo + " e tribunal " + tribunal + " exigem cards, atalhos, criticidade e detalhe diferentes");
        if (ramo.equals("PENAL")) {
            alertas.add("no penal, urgência, liberdade, sigilo, vista ministerial e defesa técnica não podem dividir a mesma densidade visual do cível comum");
        }
        if (ramo.equals("TRABALHISTA")) {
            alertas.add("no trabalhista, execução, depósito, BNDT e impulso recursal devem aparecer como núcleo do cockpit e não como detalhe escondido");
        }
        if (ramo.equals("ELEITORAL")) {
            alertas.add("no eleitoral, o shell precisa ser mais comprimido e crítico porque a janela de prazo e a especialidade do tribunal são mais agressivas");
        }
        if (ramo.equals("MILITAR")) {
            alertas.add("no militar, plantão, formalidade, sigilo e órgão julgador especializado precisam dominar o contexto visual e operacional");
        }
        if (perfil.equals("CIDADAO")) {
            alertas.add("no cidadão, o shell continua externo, enxuto e restrito a processo próprio, sem cards internos de secretaria, magistratura ou estratégia técnica");
        }
        if (perfil.equals("SECRETARIA") || perfil.equals("MAGISTRATURA")) {
            alertas.add("na operação interna, detalhe expandível não pode esconder queue, governance, pauta, preview, intimação, conclusão ou risco de vencimento");
        }
        return List.copyOf(alertas);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        String perfil = perfilAtuacao(request);
        String ramo = ramoAxis(request);
        String tribunal = tribunalAxis(request);
        return "aplicar uma matriz fina de política visual e operacional para que o processo ativo troque criticidade, vocabulário, cards primários, atalhos e densidade conforme perfil "
                + perfil
                + ", rito "
                + ramo
                + " e tribunal "
                + tribunal
                + ", sem ruído visual, sem painel paralelo e com retorno limpo à home do PJB.";
    }

    private static List<String> cardsPrimarios(RecursalAutomationRequest request) {
        ArrayList<String> cards = new ArrayList<>();
        String perfil = perfilAtuacao(request);
        String ramo = ramoAxis(request);
        cards.add(RecursalWorkbenchSurfaceCatalog.processoPrazoReal());
        cards.add(RecursalWorkbenchSurfaceCatalog.calendarPanel());
        cards.add(RecursalWorkbenchSurfaceCatalog.processualPainelContextualRotaTatica());
        switch (perfil) {
            case "CIDADAO" -> {
                cards.add(RecursalWorkbenchSurfaceCatalog.citizenProcessOverview());
                cards.add(RecursalWorkbenchSurfaceCatalog.citizenTimelineVisual());
            }
            case "DEFENSORIA" -> {
                cards.add(RecursalWorkbenchSurfaceCatalog.defensoriaExecutiveDashboard());
                cards.add(ramo.equals("PENAL") ? RecursalWorkbenchSurfaceCatalog.defensoriaHabeasCorpus() : RecursalWorkbenchSurfaceCatalog.defensoriaDefesa());
            }
            case "PROCURADORIA" -> {
                cards.add(RecursalWorkbenchSurfaceCatalog.procuradoriaExecutiveDashboard());
                cards.add(RecursalWorkbenchSurfaceCatalog.procuradoriaRecurso());
            }
            case "MINISTERIO_PUBLICO" -> {
                cards.add(RecursalWorkbenchSurfaceCatalog.ministerioPublicoPainel());
                cards.add(RecursalWorkbenchSurfaceCatalog.ministerioPublicoParecer());
            }
            case "SECRETARIA" -> {
                cards.add(RecursalWorkbenchSurfaceCatalog.secretariatQueuePanel());
                cards.add(RecursalWorkbenchSurfaceCatalog.secretariatQueueGovernance());
            }
            case "MAGISTRATURA" -> {
                cards.add(RecursalWorkbenchSurfaceCatalog.magistraturaWorkspace());
                cards.add(RecursalWorkbenchSurfaceCatalog.magistraturaPreview());
            }
            default -> {
                cards.add(RecursalWorkbenchSurfaceCatalog.officeWorkspaceExecutiveDashboard());
                cards.add(RecursalWorkbenchSurfaceCatalog.processualParticipacaoWorkspace());
            }
        }
        if (ramo.equals("TRABALHISTA")) {
            cards.add(RecursalWorkbenchSurfaceCatalog.processualPainelContextualBndt());
        }
        if (ramo.equals("PREVIDENCIARIO")) {
            cards.add(RecursalWorkbenchSurfaceCatalog.processualPainelContextualTrilhoPrevidenciario());
        }
        return List.copyOf(cards);
    }

    private static List<String> atalhosPrimarios(RecursalAutomationRequest request) {
        ArrayList<String> atalhos = new ArrayList<>();
        String perfil = perfilAtuacao(request);
        String ramo = ramoAxis(request);
        switch (perfil) {
            case "CIDADAO" -> atalhos.add(RecursalWorkbenchSurfaceCatalog.calendarNotificationPreview());
            case "DEFENSORIA" -> {
                atalhos.add(RecursalWorkbenchSurfaceCatalog.processualParticipacaoProtocolar());
                atalhos.add(RecursalWorkbenchSurfaceCatalog.peticionamentoWizardProtocoloSimples());
                atalhos.add(ramo.equals("PENAL") ? RecursalWorkbenchSurfaceCatalog.defensoriaHabeasCorpus() : RecursalWorkbenchSurfaceCatalog.defensoriaDefesa());
            }
            case "PROCURADORIA" -> {
                atalhos.add(RecursalWorkbenchSurfaceCatalog.procuradoriaRecurso());
                atalhos.add(RecursalWorkbenchSurfaceCatalog.procuradoriaParecer());
            }
            case "MINISTERIO_PUBLICO" -> {
                atalhos.add(RecursalWorkbenchSurfaceCatalog.ministerioPublicoManifestacao());
                atalhos.add(RecursalWorkbenchSurfaceCatalog.ministerioPublicoRecurso());
            }
            case "SECRETARIA" -> {
                atalhos.add(RecursalWorkbenchSurfaceCatalog.secretariatOperationalIntimacao());
                atalhos.add(RecursalWorkbenchSurfaceCatalog.secretariatOperationalConclusao());
            }
            case "MAGISTRATURA" -> {
                atalhos.add(RecursalWorkbenchSurfaceCatalog.magistraturaPreview());
                atalhos.add(RecursalWorkbenchSurfaceCatalog.julgamentoVotesStream());
            }
            default -> {
                atalhos.add(RecursalWorkbenchSurfaceCatalog.officeProcessReadingMode());
                atalhos.add(RecursalWorkbenchSurfaceCatalog.processualParticipacaoProtocolar());
            }
        }
        if (ramo.equals("TRABALHISTA")) {
            atalhos.add(RecursalWorkbenchSurfaceCatalog.secretariatOperationalLabourExecucao());
        }
        if (ramo.equals("ELEITORAL")) {
            atalhos.add(RecursalWorkbenchSurfaceCatalog.secretariatOperationalElectoralCorregedoria());
        }
        if (ramo.equals("MILITAR")) {
            atalhos.add(RecursalWorkbenchSurfaceCatalog.secretariatOperationalMilitaryPlantao());
        }
        return List.copyOf(atalhos);
    }

    private static String criticidade(RecursalAutomationRequest request) {
        String perfil = perfilAtuacao(request);
        String ramo = ramoAxis(request);
        String base = switch (perfil) {
            case "CIDADAO" -> "externa-enxuta";
            case "SECRETARIA", "MAGISTRATURA" -> "operacional-alta";
            default -> "tatico-profissional";
        };
        if (ramo.equals("PENAL") || ramo.equals("ELEITORAL") || ramo.equals("MILITAR")) {
            return base + "+criticidade-maxima";
        }
        if (ramo.equals("TRABALHISTA")) {
            return base + "+execucao-deposito";
        }
        return base + "+padrao-especializado";
    }

    private static String densidade(RecursalAutomationRequest request) {
        String perfil = perfilAtuacao(request);
        if (perfil.equals("CIDADAO")) {
            return "baixa densidade, cards curtos, detalhe mínimo e linguagem externa";
        }
        if (perfil.equals("SECRETARIA") || perfil.equals("MAGISTRATURA")) {
            return "alta densidade operacional, detalhe expandível imediato e sem esconder quick actions críticas";
        }
        return "densidade média orientada por prazo, risco, protocolo e contraditório";
    }

    private static String perfilAtuacao(RecursalAutomationRequest request) {
        if (request.perfilAtuacao() == null || request.perfilAtuacao().isBlank()) {
            return "REPRESENTACAO_TECNICA";
        }
        return request.perfilAtuacao().trim().toUpperCase();
    }

    private static String ramoAxis(RecursalAutomationRequest request) {
        if (request.ramoProcessual() == null || request.ramoProcessual().isBlank()) {
            return "CIVEL";
        }
        return request.ramoProcessual().trim().toUpperCase();
    }

    private static String tribunalAxis(RecursalAutomationRequest request) {
        if (request.juizadoEspecial()) {
            return "TURMA_RECURSAL";
        }
        String segmento = request.segmentoJudiciario() == null ? "" : request.segmentoJudiciario().trim().toUpperCase();
        return switch (segmento) {
            case "FEDERAL" -> "TRF";
            case "TRABALHISTA" -> "TRT";
            case "ELEITORAL" -> "TRE";
            case "MILITAR" -> "JUSTICA_MILITAR";
            case "SUPERIOR", "STJ", "STF", "TST", "TSE", "STM" -> segmento;
            default -> "TJ";
        };
    }
}
