package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalProceduralTaxonomyBlueprint {

    private RecursalProceduralTaxonomyBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                RecursalFormalSectionLabels.TAXONOMIA_CNJ_CLASSES_ASSUNTOS_MOVIMENTOS,
                RecursalFormalSectionLabels.DE_PARA_EQUIVALENCIA_CLASSES,
                RecursalFormalSectionLabels.TIPO_PETICAO_E_RITO_OPERACIONAL,
                RecursalFormalSectionLabels.CLASSIFICACAO_PROCESSUAL_MAIS_ESPECIFICA
        );
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        String ramo = ramoAxis(request);
        checklist.put("ALINHAR_CLASSE_CNJ", "classificar a espécie recursal na TPU sem criar classe paralela, preservando a natureza exaustiva da tabela nacional e o de-para entre legado e malha recursal ativa");
        checklist.put("ALINHAR_ASSUNTO_MAIS_ESPECIFICO", "complementar o recurso com o assunto material e, quando cabível, com matéria processual no nível mais específico do ramo " + ramo + " sem cair em assunto genérico de baixa utilidade analítica");
        checklist.put("ALINHAR_MOVIMENTACAO_REAL", "registrar somente movimentos que reflitam ato efetivamente ocorrido, com complemento livre, identificador ou tabelado quando necessário, sem expectativa fictícia de andamento futuro");
        checklist.put("ALINHAR_TIPO_PETICAO_CNJ", "classificar o protocolo recursal conforme o tipo de petição CNJ mais próximo da peça concreta: " + String.join(" | ", tiposPeticao(recursoPrincipal)));
        checklist.put("PRESERVAR_RECURSOS_EXTERNOS_PROCESSUAIS", "quando o protocolo subir para tribunal, complementar os assuntos materiais com matéria processual recursal e manter a espécie " + recursoPrincipal + " coerente no workspace e no painel contextual");
        checklist.put("REUSAR_LEGADO_SEM_DRIFT_TAXONOMICO", "reaproveitar equivalência de classes e assuntos do legado, mantendo rastreabilidade entre PJe, eproc, consulta pública e malha recursal sem duplicar vocabulário institucional");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        alertas.add("a taxonomia recursal precisa permanecer aderente às tabelas unificadas de classes, assuntos, movimentos e tipos de petição para que distribuição, estatística e leitura externa não se desalinhem");
        alertas.add("não tratar toda manifestação recursal como PETIÇÃO_OUTRAS; " + recursoPrincipal + " precisa nascer com espécie própria, checklist formal e tipologia de protocolo compatível");
        if (request.recursoPrincipalInterposto()) {
            alertas.add("como já existe recurso principal interposto, a janela de contrarrazões deve herdar a classificação material do caso e a classificação processual do protocolo recursal subsequente");
        }
        if (ramoAxis(request).equals("PENAL") || ramoAxis(request).equals("ELEITORAL") || ramoAxis(request).equals("MILITAR")) {
            alertas.add("o ramo " + ramoAxis(request) + " exige mais cuidado com assunto material sensível e sigilo graduado antes da publicação de movimentos externos");
        }
        return List.copyOf(alertas);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "alinhar a malha recursal às tabelas unificadas do CNJ, classificando classe, assunto, movimento e tipo de petição com granularidade suficiente para "
                + recursoPrincipal
                + " operar sem drift taxonômico entre legado, protocolo, painel contextual e leitura pública autenticada.";
    }

    private static List<String> tiposPeticao(String recursoPrincipal) {
        return switch (recursoPrincipal) {
            case "APELACAO" -> List.of("APELACAO", "CONTRARRAZOES", "RECURSO_ADESIVO", "MEMORIAIS");
            case "AGRAVO_DE_INSTRUMENTO" -> List.of("AGRAVO", "PETICAO_OUTRAS", "MEMORIAIS");
            case "AGRAVO_INTERNO" -> List.of("AGRAVO", "MEMORIAIS", "CONTRARRAZOES");
            case "EMBARGOS_DECLARACAO" -> List.of("EMBARGOS_DECLARACAO", "PETICAO_OUTRAS");
            case "EMBARGOS_DIVERGENCIA" -> List.of("EMBARGOS_DIVERGENCIA", "MEMORIAIS", "PARECER");
            case "RECURSO_INOMINADO" -> List.of("RECURSO_INOMINADO", "CONTRARRAZOES");
            case "RECURSO_ESPECIAL" -> List.of("RECURSO_ESPECIAL", "CONTRARRAZOES", "PARECER");
            case "RECURSO_EXTRAORDINARIO" -> List.of("RECURSO_EXTRAORDINARIO", "CONTRARRAZOES", "PARECER");
            default -> List.of("PETICAO_OUTRAS", "CONTRARRAZOES", "MEMORIAIS");
        };
    }

    private static String ramoAxis(RecursalAutomationRequest request) {
        if (request.ramoProcessual() == null || request.ramoProcessual().isBlank()) {
            return "CIVEL";
        }
        return request.ramoProcessual().trim().toUpperCase();
    }
}
