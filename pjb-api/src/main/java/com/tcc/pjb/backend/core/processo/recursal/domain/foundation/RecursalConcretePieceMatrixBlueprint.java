package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RecursalConcretePieceMatrixBlueprint {

    private RecursalConcretePieceMatrixBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> sections = new ArrayList<>();
        sections.add(RecursalFormalSectionLabels.MATRIZ_PECAS_CONCRETAS_POR_ATOR);
        sections.add(RecursalFormalSectionLabels.ADVOCACIA_PRIVADA_RECURSAL_CONTRARRAZOES_CONTRAMINUTA);
        sections.add(RecursalFormalSectionLabels.DEFENSORIA_RECURSAL_DEFESA_HABEAS_CORPUS_CONTRARRAZOES);
        sections.add(RecursalFormalSectionLabels.PROCURADORIA_RECURSAL_RECURSO_PARECER_CONTESTACAO);
        sections.add(RecursalFormalSectionLabels.MINISTERIO_PUBLICO_RECURSAL_MANIFESTACAO_PARECER_PROMOCAO);
        sections.add(RecursalFormalSectionLabels.PECAS_COMPLEMENTARES_MEMORIAIS_QESITOS_RESPOSTA_LAUDO);
        sections.add(RecursalFormalSectionLabels.CIVEL_JUIZADOS_PECAS_CONCRETAS);
        sections.add(RecursalFormalSectionLabels.PENAL_PECAS_CONCRETAS);
        sections.add(RecursalFormalSectionLabels.TRABALHISTA_PECAS_CONCRETAS);
        sections.add(RecursalFormalSectionLabels.ELEITORAL_PECAS_CONCRETAS);
        sections.add(RecursalFormalSectionLabels.MILITAR_PECAS_CONCRETAS);
        return List.copyOf(sections);
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("ABRIR_ATOR_POSTULANTE_E_CAPACIDADE", "antes de protocolar a peça " + recursoPrincipal + ", classificar o ator jurídico habilitado e reaproveitar a superfície adequada, sem duplicação: "
                + String.join(" | ", superficesAtorBase()));
        checklist.put("ADVOCACIA_PRIVADA_E_REPRESENTACAO", "na advocacia privada e no modo escritório, reutilizar studio, participação ativa e dashboards para abrir recurso, contrarrazões, contraminuta, memoriais e petições intercorrentes recursais, preservando assinatura, leitura e protocolo governado");
        checklist.put("DEFENSORIA_PUBLICA", "na Defensoria, distinguir defesa, habeas corpus, recurso, contrarrazões e assistência gratuita estratégica; a malha recursal deve plugar snapshot, malha do processo e rotas próprias: "
                + String.join(" | ", superficesDefensoria()));
        checklist.put("PROCURADORIA_PUBLICA", "na Procuradoria, separar contestação, recurso, parecer e execução fiscal quando houver reflexo recursal, reutilizando o eixo operacional já existente: "
                + String.join(" | ", superficesProcuradoria()));
        checklist.put("MINISTERIO_PUBLICO", "no Ministério Público, distinguir manifestação, parecer, recurso e requisição de diligência; o sistema não pode reduzir a atuação ministerial a um recurso genérico: "
                + String.join(" | ", superficesMinisterioPublico()));
        checklist.put("PECAS_COMPLEMENTARES_E_AUXILIARES", "resposta a laudo, quesitos, memoriais, promoção de diligência e peças complementares recursais devem usar a mesma malha de auxiliares e participação ativa: "
                + String.join(" | ", superficesComplementares()));
        checklist.put("ADAPTAR_PECAS_POR_RAMO", "para o ramo " + familiaRamo(request) + ", o PJB deve ajustar espécie, prazo, contraditório, órgão e peça concreta, diferenciando " + resumoRamoAtual(request));
        checklist.put("NACIONALIZAR_SEM_DUPLICAR", "a matriz recursal nacional deve reaproveitar a espinha base e apenas trocar o perfil da peça; é vedado duplicar a petição inicial como sucedâneo de recurso, embargos, parecer ou contrarrazões em qualquer rito");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alerts = new ArrayList<>();
        alerts.add("o PJB deve reconhecer peça concreta por ator e por rito; recurso, embargos, parecer, manifestação, contrarrazões e memoriais não são a mesma coisa");
        alerts.add("o ramo " + familiaRamo(request) + " exige diferenciação adicional de legitimado, contraditório e semântica da peça, especialmente fora do cível comum");
        alerts.add(descricaoAtorRamo(request));
        return List.copyOf(alerts);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "reutilizar o studio e a participação ativa já existentes para abrir peças concretas por ator e por rito — recurso, embargos, contrarrazões, parecer, manifestação, memoriais, quesitos e resposta a laudo — sem duplicar a espinha do peticionamento base na rota "
                + recursoPrincipal + " do contexto " + familiaRamo(request) + '.';
    }

    private static List<String> superficesAtorBase() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.peticionamentoStudioWorkspace(),
                RecursalWorkbenchSurfaceCatalog.peticionamentoStudioQuickDraft(),
                RecursalWorkbenchSurfaceCatalog.peticionamentoStudioGovernedReview(),
                RecursalWorkbenchSurfaceCatalog.peticionamentoWizardProtocoloSimples(),
                RecursalWorkbenchSurfaceCatalog.processualParticipacaoWorkspace(),
                RecursalWorkbenchSurfaceCatalog.processualParticipacaoProtocolar(),
                RecursalWorkbenchSurfaceCatalog.processualParticipacaoSubmissoes()
        );
    }

    private static List<String> superficesDefensoria() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.defensoriaExecutiveDashboard(),
                RecursalWorkbenchSurfaceCatalog.defensoriaOrganDashboard(),
                RecursalWorkbenchSurfaceCatalog.defensoriaSnapshot(),
                RecursalWorkbenchSurfaceCatalog.defensoriaMalha(),
                RecursalWorkbenchSurfaceCatalog.defensoriaDefesa(),
                RecursalWorkbenchSurfaceCatalog.defensoriaHabeasCorpus()
        );
    }

    private static List<String> superficesProcuradoria() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.procuradoriaExecutiveDashboard(),
                RecursalWorkbenchSurfaceCatalog.procuradoriaOrganDashboard(),
                RecursalWorkbenchSurfaceCatalog.procuradoriaSnapshot(),
                RecursalWorkbenchSurfaceCatalog.procuradoriaMalha(),
                RecursalWorkbenchSurfaceCatalog.procuradoriaRecurso(),
                RecursalWorkbenchSurfaceCatalog.procuradoriaParecer()
        );
    }

    private static List<String> superficesMinisterioPublico() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.ministerioPublicoPainel(),
                RecursalWorkbenchSurfaceCatalog.ministerioPublicoMalha(),
                RecursalWorkbenchSurfaceCatalog.ministerioPublicoManifestacao(),
                RecursalWorkbenchSurfaceCatalog.ministerioPublicoParecer(),
                RecursalWorkbenchSurfaceCatalog.ministerioPublicoRecurso()
        );
    }

    private static List<String> superficesComplementares() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.peritoOperacionalLaudo(),
                RecursalWorkbenchSurfaceCatalog.peritoQuesitos(),
                RecursalWorkbenchSurfaceCatalog.oficialJusticaProcessWorkbench(),
                RecursalWorkbenchSurfaceCatalog.oficialJusticaOficios(),
                RecursalWorkbenchSurfaceCatalog.oficialJusticaOficiosResposta(),
                RecursalWorkbenchSurfaceCatalog.processualPautaAudiencia()
        );
    }

    private static String resumoRamoAtual(RecursalAutomationRequest request) {
        return switch (normalizedRamo(request)) {
            case "PENAL" -> "apelação criminal, recurso em sentido estrito, carta testemunhável, memoriais, razões/contrarrazões criminais, habeas corpus estratégico e manifestações ministeriais penais";
            case "TRABALHISTA" -> "recurso ordinário, agravo de petição, recurso de revista, agravo de instrumento, contrarrazões trabalhistas, memoriais e respostas executórias do ramo";
            case "ELEITORAL" -> "recurso eleitoral, recurso especial eleitoral, recurso ordinário eleitoral, agravos, embargos de declaração, pareceres e manifestações do Ministério Público Eleitoral";
            case "MILITAR" -> "apelação militar, recurso em sentido estrito militar, embargos infringentes e de nulidade, correição parcial, memoriais e peças criminais castrenses";
            case "CIVEL" -> request != null && request.juizadoEspecial()
                    ? "recurso inominado, pedido de uniformização, embargos, contrarrazões e petições da turma recursal"
                    : "apelação, agravos, embargos, recursos excepcionais, adesivo, contrarrazões, contraminuta e memoriais cíveis";
            default -> "peças recursais e institucionais multirramo com classificação conservadora antes do protocolo";
        };
    }

    private static String descricaoAtorRamo(RecursalAutomationRequest request) {
        return switch (normalizedRamo(request)) {
            case "PENAL" -> "no penal, defensor, MP e advocacia precisam de peças distintas para resposta criminal, recurso, razões, contrarrazões, habeas corpus e memoriais";
            case "TRABALHISTA" -> "no trabalhista, advocacia, sindicatos e entes públicos demandam separação entre razões, contrarrazões, agravo, revista e peças executórias";
            case "ELEITORAL" -> "no eleitoral, a peça do candidato, partido, coligação, federação ou Ministério Público Eleitoral não pode ser confundida com a do cível comum";
            case "MILITAR" -> "no militar, a peça deve refletir a semântica castrense e a organização da Justiça Militar, sem reaproveitamento cego do CPP ou do CPC";
            default -> "quando o ramo não vier explícito, o PJB deve travar a classificação da peça concreta antes da assinatura e do protocolo";
        };
    }

    private static String familiaRamo(RecursalAutomationRequest request) {
        String ramo = normalizedRamo(request);
        if (request != null && request.juizadoEspecial()) {
            return ramo + "/JUIZADO_ESPECIAL";
        }
        return ramo;
    }

    private static String normalizedRamo(RecursalAutomationRequest request) {
        if (request == null || request.ramoProcessual() == null || request.ramoProcessual().isBlank()) {
            return "MULTIRRAMO";
        }
        return request.ramoProcessual().trim().toUpperCase(Locale.ROOT);
    }
}
