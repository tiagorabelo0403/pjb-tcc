package com.tcc.pjb.backend.ai.academy;

import java.util.List;

public record CurriculumSnapshot(
        String ramoCodigo,
        String nome,
        List<String> materiasPrioritarias,
        List<String> legislacaoChave,
        List<String> principiosChave,
        List<String> prazosCriticos,
        List<String> ritosRelacionados
) {
}
