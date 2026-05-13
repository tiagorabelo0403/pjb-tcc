package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalActorContextExperienceBlueprint {

    private RecursalActorContextExperienceBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                RecursalFormalSectionLabels.SHELL_CONTEXTUAL_POR_ATOR,
                RecursalFormalSectionLabels.PERFIL_ATUACAO_CONTEXTUAL,
                RecursalFormalSectionLabels.PROFILE_CODE_CONTEXTO_ATOR,
                RecursalFormalSectionLabels.CARDS_RISCO_POR_ATOR,
                RecursalFormalSectionLabels.QUICK_ACTIONS_POR_ATOR,
                RecursalFormalSectionLabels.LINGUAGEM_OPERACIONAL_POR_ATOR,
                RecursalFormalSectionLabels.OBSERVABILIDADE_CONTEXTO_ATOR
        );
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        String perfil = perfilAtuacao(request);
        String ramo = ramoAxis(request);
        String profileCode = profileCode(request);
        checklist.put("RESOLVER_PERFIL_ATOR", "resolver o perfil contextual do usuário para o processo ativo: perfil=" + perfil + ", ramo=" + ramo + ", espécie=" + recursoPrincipal);
        checklist.put("APLICAR_PROFILE_CODE", "carregar o shell contextual por ator usando profileCode=" + profileCode + " na malha unificada do processo ativo: " + RecursalWorkbenchSurfaceCatalog.processualPainelContextual());
        checklist.put("PRIORIZAR_CARDS_RISCO", "exibir cards de risco e prioridade próprios do ator para o rito ativo: " + String.join(" | ", actorRiskCards(request)));
        checklist.put("ABRIR_QUICK_ACTIONS", "abrir quick actions realmente úteis para o ator no contexto do processo ativo: " + String.join(" | ", actorQuickActions(request)));
        checklist.put("AJUSTAR_LINGUAGEM", "trocar a linguagem operacional para o vocabulário do ator, preservando a especialidade do rito " + ramo + " e evitando microcopy genérica");
        checklist.put("LIGAR_OBSERVABILIDADE", "amarrar telemetria, fontes oficiais, prazo real e rota tática ao contexto do ator, sem criar dashboard paralelo: " + String.join(" | ", actorObservabilitySurfaces(request)));
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        String perfil = perfilAtuacao(request);
        String ramo = ramoAxis(request);
        alertas.add("o shell do processo ativo deve mudar por ator e por rito, não apenas por filtro de lista ou nome do painel");
        alertas.add("o perfil " + perfil + " precisa receber cards de risco, quick actions e linguagem institucional próprios para o ramo " + ramo);
        switch (perfil) {
            case "DEFENSORIA" -> {
                if (ramo.equals("PENAL")) {
                    alertas.add("na defensoria penal, habeas corpus, urgência, defesa, ciência imediata e movimentação sigilosa devem subir para o topo do shell contextual");
                } else {
                    alertas.add("na defensoria, defesa técnica, contrarrazões, prazo vivo e protocolo assistido devem ficar acima de cards gerenciais genéricos");
                }
            }
            case "PROCURADORIA" -> {
                if (ramo.equals("TRABALHISTA")) {
                    alertas.add("na procuradoria trabalhista, execução, depósito e impulso recursal devem ficar acima do cockpit cível comum");
                } else {
                    alertas.add("na procuradoria, parecer, recurso, resposta institucional e risco de prazo devem ser priorizados no processo ativo");
                }
            }
            case "MINISTERIO_PUBLICO" -> alertas.add("no Ministério Público, manifestação, parecer, promoção e impulso acusatório/fiscal devem aparecer com vocabulário próprio e criticidade alta");
            case "SECRETARIA" -> alertas.add("na secretaria, cards de queue, agenda, governance, coverage, intimação e conclusão precisam dominar o shell contextual do processo ativo");
            case "MAGISTRATURA" -> alertas.add("na magistratura, preview, ato, voto, pauta, acórdão e quick actions decisórias devem prevalecer sobre cards externos");
            case "CIDADAO" -> alertas.add("no cidadão, o shell deve continuar enxuto, restrito a processo próprio, última movimentação, cor processual e avisos externos compatíveis com sigilo");
            default -> alertas.add("na representação técnica, leitura, protocolo, peticionamento, contrarrazões, preparo e criticidade recursal devem aparecer antes de analytics acessórios");
        }
        return List.copyOf(alertas);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        String perfil = perfilAtuacao(request);
        String ramo = ramoAxis(request);
        return "ajustar o shell contextual do processo ativo por ator, com profileCode determinístico, cards de risco, quick actions, linguagem institucional e observabilidade amarrados ao perfil "
                + perfil
                + " no rito "
                + ramo
                + ", sem abrir painel paralelo nem contaminar a home do PJB.";
    }

    private static List<String> actorRiskCards(RecursalAutomationRequest request) {
        String perfil = perfilAtuacao(request);
        String ramo = ramoAxis(request);
        ArrayList<String> cards = new ArrayList<>();
        cards.add(RecursalWorkbenchSurfaceCatalog.processoPrazoReal());
        cards.add(RecursalWorkbenchSurfaceCatalog.calendarPanel());
        switch (perfil) {
            case "CIDADAO" -> {
                cards.add(RecursalWorkbenchSurfaceCatalog.citizenProcessOverview());
                cards.add(RecursalWorkbenchSurfaceCatalog.citizenTimelineVisual());
            }
            case "DEFENSORIA" -> {
                cards.add(RecursalWorkbenchSurfaceCatalog.defensoriaExecutiveDashboard());
                cards.add(RecursalWorkbenchSurfaceCatalog.defensoriaMalha());
                cards.add(ramo.equals("PENAL") ? RecursalWorkbenchSurfaceCatalog.defensoriaHabeasCorpus() : RecursalWorkbenchSurfaceCatalog.defensoriaDefesa());
            }
            case "PROCURADORIA" -> {
                cards.add(RecursalWorkbenchSurfaceCatalog.procuradoriaExecutiveDashboard());
                cards.add(RecursalWorkbenchSurfaceCatalog.procuradoriaMalha());
                cards.add(RecursalWorkbenchSurfaceCatalog.procuradoriaRecurso());
            }
            case "MINISTERIO_PUBLICO" -> {
                cards.add(RecursalWorkbenchSurfaceCatalog.ministerioPublicoPainel());
                cards.add(RecursalWorkbenchSurfaceCatalog.ministerioPublicoMalha());
                cards.add(RecursalWorkbenchSurfaceCatalog.ministerioPublicoRecurso());
            }
            case "SECRETARIA" -> {
                cards.add(RecursalWorkbenchSurfaceCatalog.secretariatQueuePanel());
                cards.add(RecursalWorkbenchSurfaceCatalog.secretariatQueueGovernance());
                cards.add(RecursalWorkbenchSurfaceCatalog.secretariatOperationalSnapshot());
            }
            case "MAGISTRATURA" -> {
                cards.add(RecursalWorkbenchSurfaceCatalog.magistraturaWorkspace());
                cards.add(RecursalWorkbenchSurfaceCatalog.magistraturaPreview());
                cards.add(RecursalWorkbenchSurfaceCatalog.processualPendenciasPainel());
            }
            default -> {
                cards.add(RecursalWorkbenchSurfaceCatalog.officeWorkspaceExecutiveDashboard());
                cards.add(RecursalWorkbenchSurfaceCatalog.processualParticipacaoWorkspace());
                cards.add(RecursalWorkbenchSurfaceCatalog.officeProcessReadingMode());
            }
        }
        return List.copyOf(cards);
    }

    private static List<String> actorQuickActions(RecursalAutomationRequest request) {
        String perfil = perfilAtuacao(request);
        String ramo = ramoAxis(request);
        ArrayList<String> actions = new ArrayList<>();
        switch (perfil) {
            case "CIDADAO" -> actions.add(RecursalWorkbenchSurfaceCatalog.calendarNotificationPreview());
            case "DEFENSORIA" -> {
                actions.add(RecursalWorkbenchSurfaceCatalog.processualParticipacaoProtocolar());
                actions.add(ramo.equals("PENAL") ? RecursalWorkbenchSurfaceCatalog.defensoriaHabeasCorpus() : RecursalWorkbenchSurfaceCatalog.defensoriaDefesa());
                actions.add(RecursalWorkbenchSurfaceCatalog.peticionamentoWizardProtocoloSimples());
            }
            case "PROCURADORIA" -> {
                actions.add(RecursalWorkbenchSurfaceCatalog.procuradoriaRecurso());
                actions.add(RecursalWorkbenchSurfaceCatalog.procuradoriaParecer());
                actions.add(RecursalWorkbenchSurfaceCatalog.peticionamentoWizardProtocoloSimples());
            }
            case "MINISTERIO_PUBLICO" -> {
                actions.add(RecursalWorkbenchSurfaceCatalog.ministerioPublicoManifestacao());
                actions.add(RecursalWorkbenchSurfaceCatalog.ministerioPublicoParecer());
                actions.add(RecursalWorkbenchSurfaceCatalog.ministerioPublicoRecurso());
            }
            case "SECRETARIA" -> {
                actions.add(RecursalWorkbenchSurfaceCatalog.secretariatOperationalJuntada());
                actions.add(RecursalWorkbenchSurfaceCatalog.secretariatOperationalIntimacao());
                actions.add(RecursalWorkbenchSurfaceCatalog.secretariatOperationalConclusao());
            }
            case "MAGISTRATURA" -> {
                actions.add(RecursalWorkbenchSurfaceCatalog.magistraturaPreview());
                actions.add(RecursalWorkbenchSurfaceCatalog.julgamentoVotesStream());
                actions.add(RecursalWorkbenchSurfaceCatalog.institutionalWorkbenchActionPreview());
            }
            default -> {
                actions.add(RecursalWorkbenchSurfaceCatalog.processualParticipacaoProtocolar());
                actions.add(RecursalWorkbenchSurfaceCatalog.peticionamentoWizardProtocoloSimples());
                actions.add(RecursalWorkbenchSurfaceCatalog.peticionamentoJourneyInteligente());
            }
        }
        return List.copyOf(actions);
    }

    private static List<String> actorObservabilitySurfaces(RecursalAutomationRequest request) {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.processualPainelContextualTelemetria(),
                RecursalWorkbenchSurfaceCatalog.processualPainelContextualFontesOficiais(),
                RecursalWorkbenchSurfaceCatalog.processualPainelContextualRotaTatica(),
                RecursalWorkbenchSurfaceCatalog.processoPrazoReal()
        );
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

    private static String profileCode(RecursalAutomationRequest request) {
        String segmento = request.segmentoJudiciario() == null || request.segmentoJudiciario().isBlank()
                ? "ESTADUAL"
                : request.segmentoJudiciario().trim().toUpperCase();
        return perfilAtuacao(request) + "-" + ramoAxis(request) + "-" + segmento + (request.juizadoEspecial() ? "-JUIZADO" : "");
    }
}
