package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RecursalTribunalDifferentiationBlueprint {

    private RecursalTribunalDifferentiationBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> sections = new ArrayList<>();
        sections.add(RecursalFormalSectionLabels.COMPORTAMENTO_RECURSAL_POR_TRIBUNAL_E_ORGAO);
        sections.add(RecursalFormalSectionLabels.MATRIZ_PRAZOS_POR_RITO_E_RECURSO);
        sections.add(RecursalFormalSectionLabels.MATRIZ_PREPARO_DEPOSITO_E_CUSTAS);
        sections.add(RecursalFormalSectionLabels.FILTRO_TRIBUNAL_ORGAO_FRACIONARIO_COMPETENTE);
        sections.add(RecursalFormalSectionLabels.JUIZO_ADMISSIBILIDADE_E_FILTROS_ESPECIAIS);
        sections.add(RecursalFormalSectionLabels.REGRA_INTERRUPCAO_SUSPENSAO_E_REABERTURA_PRAZO);
        sections.add(RecursalFormalSectionLabels.DIFERENCAS_CIVEL_JUIZADOS);
        sections.add(RecursalFormalSectionLabels.DIFERENCAS_PENAL);
        sections.add(RecursalFormalSectionLabels.DIFERENCAS_TRABALHISTA);
        sections.add(RecursalFormalSectionLabels.DIFERENCAS_ELEITORAL);
        sections.add(RecursalFormalSectionLabels.DIFERENCAS_MILITAR);
        return List.copyOf(sections);
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("CLASSIFICAR_TRIBUNAL_E_ORGAO", "antes de protocolar a peça " + recursoPrincipal + ", classificar tribunal, órgão julgador, órgão fracionário e filtro interno competente para o contexto " + familia(request));
        checklist.put("TRAVAR_PRAZO_CONFORME_RITO", "aplicar prazo vivo por rito e espécie, sem herdar a régua do CPC para tudo: " + resumoPrazos(request, recursoPrincipal));
        checklist.put("DIFERENCIAR_PREPARO_E_DEPOSITO", "separar preparo, depósito recursal, isenção, gratuidade, autos eletrônicos e dispensa institucional conforme o ramo e o tribunal competente");
        checklist.put("MODELAR_ADMISSIBILIDADE_REAL", "distinguir juízo de admissibilidade no tribunal recorrido, órgão colegiado, presidência/vice, turma recursal ou corte superior, conforme a rota e o tribunal");
        checklist.put("APLICAR_TRIBUNAL_FAMILIA_COMPETENTE", "mapear o comportamento por família de tribunal sem unificar TJ/TRF/TRE/TRT/STM/STJ/STF/TSE/TST e Turma Recursal como se fossem o mesmo degrau");
        checklist.put("REABRIR_E_RECALCULAR_PRAZO", "embargos, publicação, sessão e pós-julgamento devem recalcular a janela subsequente segundo a regra do rito, inclusive quando houver interrupção, suspensão ou reabertura");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alerts = new ArrayList<>();
        alerts.add("o PJB não pode uniformizar prazo, preparo, admissibilidade e órgão julgador de todos os ritos sob a lógica do CPC comum");
        alerts.add("o contexto " + familia(request) + " exige trilha específica de tribunal/órgão e recalibração de prazo para a rota " + recursoPrincipal);
        alerts.add(descricaoEspecifica(request));
        return List.copyOf(alerts);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "diferenciar comportamento recursal por rito, tribunal e órgão julgador, aplicando prazos, filtros de admissibilidade, preparo ou depósito e pós-julgamento próprios do contexto "
                + familia(request)
                + " para a rota "
                + recursoPrincipal
                + ", sem tratar todos os tribunais e ramos como uma única malha.";
    }

    private static String resumoPrazos(RecursalAutomationRequest request, String recursoPrincipal) {
        return switch (normalizedRamo(request)) {
            case "TRABALHISTA" -> "trabalhista com prazo-base de 8 dias para recursos e contrarrazões, além de depósito recursal quando exigível";
            case "ELEITORAL" -> "eleitoral com prazo-base curto de 3 dias para recursos e embargos, além de hipóteses regimentais de 1 dia para agravo interno em procedimentos específicos";
            case "MILITAR" -> "militar com apelação em 5 dias e recurso em sentido estrito em 3 dias no CPPM";
            case "PENAL" -> "penal com regime próprio do CPP e prazo-base de 5 dias para recurso voluntário, sem herdar automaticamente o calendário cível";
            case "CIVEL" -> request != null && request.juizadoEspecial()
                    ? "juizados com recurso inominado em 10 dias e embargos em 5 dias, fora da malha da apelação cível comum"
                    : recursoPrincipal.equals("EMBARGOS_DECLARACAO")
                    ? "cível comum com embargos de declaração em 5 dias"
                    : "cível comum com 15 dias úteis para a maior parte dos recursos e 5 dias para embargos de declaração";
            default -> "prazo dependente do ramo, do tribunal e da espécie, exigindo classificação antes de protocolar";
        };
    }

    private static String descricaoEspecifica(RecursalAutomationRequest request) {
        return switch (normalizedRamo(request)) {
            case "TRABALHISTA" -> "no trabalhista, o PJB precisa separar TRT e TST, depósito recursal, admissibilidade de revista e filtros internos do ramo";
            case "ELEITORAL" -> "no eleitoral, o PJB precisa separar juiz eleitoral, TRE e TSE, porque os prazos são mais curtos e a especialidade eleitoral comprime o fluxo recursal";
            case "MILITAR" -> "no militar, o PJB precisa separar auditoria, TJM/órgão militar competente e STM, com prazo e via recursal do CPPM";
            case "PENAL" -> "no penal, o PJB precisa tratar tribunal criminal e execução penal sem importar automaticamente preparo, adesivo e semântica do cível";
            case "CIVEL" -> request != null && request.juizadoEspecial()
                    ? "nos juizados, o PJB precisa separar turma recursal e pedido de uniformização da malha da apelação e dos tribunais comuns"
                    : "no cível comum, o PJB precisa separar 1º grau, TJ/TRF, presidência/vice e cortes superiores, com a régua do CPC e da legislação especial";
            default -> "quando o ramo ou tribunal não vier explícito, o PJB deve travar classificação conservadora antes de abrir prazo, preparo e órgão destino";
        };
    }

    private static String familia(RecursalAutomationRequest request) {
        String ramo = normalizedRamo(request);
        if (request != null && request.juizadoEspecial()) {
            return ramo + "/JUIZADO_ESPECIAL";
        }
        return ramo + "/" + normalizedSegmento(request);
    }

    private static String normalizedRamo(RecursalAutomationRequest request) {
        if (request == null || request.ramoProcessual() == null || request.ramoProcessual().isBlank()) {
            return "MULTIRRAMO";
        }
        return request.ramoProcessual().trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizedSegmento(RecursalAutomationRequest request) {
        if (request == null || request.segmentoJudiciario() == null || request.segmentoJudiciario().isBlank()) {
            return "MULTITRIBUNAL";
        }
        return request.segmentoJudiciario().trim().toUpperCase(Locale.ROOT);
    }
}
