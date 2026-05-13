package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalPetitioningReuseBlueprint {

    private RecursalPetitioningReuseBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> sections = new ArrayList<>();
        sections.add(RecursalFormalSectionLabels.REUSO_STUDIO_PETICIONAMENTO_PRIMEIRO_GRAU);
        sections.add(RecursalFormalSectionLabels.PERFIL_PETICAO_RECURSAL_ESPECIFICA);
        sections.add(RecursalFormalSectionLabels.PERFIL_PETICAO_EMBARGOS_ESPECIFICA);
        sections.add(RecursalFormalSectionLabels.PERFIL_PARECER_MANIFESTACAO_INSTITUCIONAL);
        sections.add(RecursalFormalSectionLabels.MATRIZ_PECAS_RECURSAIS_POR_RAMO);
        sections.add(RecursalFormalSectionLabels.FILTRO_CAPACIDADE_POSTULATORIA_RECURSAL);
        sections.add(RecursalFormalSectionLabels.CHECKLIST_DOSSIE_DOCUMENTAL_RECURSAL);
        sections.add(RecursalFormalSectionLabels.JORNADA_PROTOCOLO_RECURSAL_CLASSIFICADA);
        return List.copyOf(sections);
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("REUSAR_STUDIO_SEM_DUPLICAR", "reaproveitar a mesma espinha do peticionamento de primeiro grau — sessão, workspace, minuta rápida, revisão governada, diff de minuta, rascunhos, wizard de protocolo e jornada inteligente — sem abrir módulo satélite: "
                + String.join(" | ", rotasStudioEFluxoBase()));
        checklist.put("CLASSIFICAR_PERFIL_DA_PECA", "o sistema deve diferenciar petição de primeiro grau, petição recursal, petição de embargos e manifestação institucional, com regra própria por espécie "
                + recursoPrincipal + " e por ramo " + familiaRamo(request) + "; ele não pode tratar tudo como PETICAO_INICIAL genérica");
        checklist.put("FILTRAR_CAPACIDADE_POSTULATORIA", "abrir a trilha apenas para quem tem poder jurídico no caso — advocacia, Defensoria, Procuradoria, Ministério Público e demais legitimados compatíveis — preservando autoria técnica, perfil institucional e degrau de visibilidade já existentes");
        checklist.put("APLICAR_MATRIZ_RECURSAL_POR_ESPECIE", "cada peça deve nascer com matriz viva de requisitos: recurso ordinário ou apelação, agravo, recurso excepcional, embargos e manifestações em contrarrazões ou pareceres devem mudar endereçamento, fundamentos, anexos, preparo, impugnação específica e protocolo");
        checklist.put("APLICAR_MATRIZ_POR_RAMO", "civil, penal, trabalhista, eleitoral, militar e microssistemas especiais devem reutilizar a mesma espinha de studio, mas com linguagem, checklist, peças, sigilo e atos de protocolo próprios do ramo");
        checklist.put("PRESERVAR_DOSSIE_EVIDENCIA_E_DOCUMENTOS", "o dossiê do studio deve reaproveitar linha do tempo, prova documental, documentos pessoais, representação, prova técnica e matriz de pedidos, mas exigindo filtro documental recursal ou de embargos antes do protocolo");
        checklist.put("GOVERNAR_PROTOCOLO_FINAL", "o wizard e a jornada inteligente devem reconhecer se a saída será recurso, embargos, contrarrazões, parecer, manifestação institucional, memoriais, quesitos complementares, resposta a laudo ou petição intercorrente recursal, sem colar a semântica da inicial");
        checklist.put("REUSAR_RASCUNHOS_COM_FORMA_CERTA", "os rascunhos existentes podem ser reutilizados, salvos, comparados e protocolados de novo, desde que o perfil documental já venha rotulado como petição recursal ou de embargos e nunca como peça inaugural");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alerts = new ArrayList<>();
        alerts.add("não duplicar o peticionamento da primeira instância; a mesma espinha precisa ser reaproveitada com classificação viva de peça e de rito");
        alerts.add("petição recursal e petição de embargos têm forma, filtro documental e lógica de protocolo próprios; o sistema precisa reconhecer isso antes da montagem da minuta");
        alerts.add("advocacia, Defensoria, Procuradoria e Ministério Público podem usar a mesma trilha base, mas com autoria técnica e manifestação institucional compatíveis com o perfil habilitado");
        alerts.add("o ramo " + familiaRamo(request) + " não pode herdar peticionamento civil de forma cega; ele deve reaproveitar studio e wizard já existentes com semântica própria do rito");
        if (recursoPrincipal.startsWith("EMBARGOS")) {
            alerts.add("embargos exigem integração dirigida ao órgão prolator, foco em vício integrativo e eventual efeito modificativo, sem aparência de petição inicial nem de recurso ordinário genérico");
        } else if (recursoPrincipal.contains("AGRAVO")) {
            alerts.add("agravos exigem petição com impugnação específica, filtro de decisão impugnada e, quando necessário, controle das peças do instrumento e do pedido urgente");
        } else if (recursoPrincipal.equals("RECURSO_ESPECIAL") || recursoPrincipal.equals("RECURSO_EXTRAORDINARIO") || recursoPrincipal.equals("AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO")) {
            alerts.add("recursos excepcionais exigem trilha mais rígida de admissibilidade, pré-questionamento, filtro de presidência ou vice e documentação de suporte mais severa");
        }
        return List.copyOf(alerts);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "reaproveitar o mesmo studio e a mesma jornada do peticionamento da primeira instância, mas classificando a saída como "
                + perfilPeticionamento(recursoPrincipal)
                + " sob a matriz do ramo "
                + familiaRamo(request)
                + ", sem duplicar fluxo, contrato ou semântica de protocolo.";
    }

    private static List<String> rotasStudioEFluxoBase() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.peticionamentoSessaoInicial(),
                RecursalWorkbenchSurfaceCatalog.peticionamentoStudioWorkspace(),
                RecursalWorkbenchSurfaceCatalog.peticionamentoStudioQuickDraft(),
                RecursalWorkbenchSurfaceCatalog.peticionamentoStudioGovernedReview(),
                RecursalWorkbenchSurfaceCatalog.peticionamentoStudioDraftDiff(),
                RecursalWorkbenchSurfaceCatalog.peticionamentoWizardProtocoloSimples(),
                RecursalWorkbenchSurfaceCatalog.peticionamentoJourneyInteligente(),
                RecursalWorkbenchSurfaceCatalog.peticionamentoInitialDraftStruct(),
                RecursalWorkbenchSurfaceCatalog.peticionamentoInitialDraftSave(),
                RecursalWorkbenchSurfaceCatalog.peticionamentoInitialDraftMine(),
                RecursalWorkbenchSurfaceCatalog.peticionamentoInitialDraftDetail(),
                RecursalWorkbenchSurfaceCatalog.peticionamentoInitialDraftProtocol()
        );
    }

    private static String perfilPeticionamento(String recursoPrincipal) {
        if (recursoPrincipal.startsWith("EMBARGOS")) {
            return "PETICAO_EMBARGOS";
        }
        return switch (recursoPrincipal) {
            case "APELACAO", "RECURSO_INOMINADO", "AGRAVO_DE_INSTRUMENTO", "AGRAVO_INTERNO", "RECURSO_ESPECIAL", "RECURSO_EXTRAORDINARIO", "AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO" -> "PETICAO_RECURSAL";
            default -> "MANIFESTACAO_RECURSAL_CLASSIFICADA";
        };
    }

    private static String familiaRamo(RecursalAutomationRequest request) {
        if (request == null || request.ramoProcessual() == null || request.ramoProcessual().isBlank()) {
            return "RAMO_NAO_MAPEADO";
        }
        String ramo = request.ramoProcessual().trim().toUpperCase();
        if (request.juizadoEspecial()) {
            return ramo + "/JUIZADO_ESPECIAL";
        }
        return ramo;
    }
}
