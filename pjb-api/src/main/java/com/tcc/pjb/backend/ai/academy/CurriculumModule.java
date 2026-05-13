package com.tcc.pjb.backend.ai.academy;

import java.util.List;

public record CurriculumModule(
        String ramo,
        String nome,
        List<String> materias,
        List<String> legislacaoPrincipal,
        List<String> principiosGerais,
        List<String> sumulas,
        List<String> temasRepercussaoGeral,
        List<String> topicosProcessuais,
        List<String> topicosSubstantivos
) {
}
