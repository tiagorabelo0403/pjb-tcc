package com.tcc.pjb.backend.core.comunicacao.institucional.canonico;

import java.util.List;
import com.tcc.pjb.backend.model.entity.enums.AtoCanonicoProcessual;

public record ResolucaoAtoCanonicoResult(
        AtoCanonicoProcessual atoCanonico,
        PoliticaAtoCanonicoProcessual politica,
        int score,
        List<String> justificativas,
        String hashResolucao
) {
    public ResolucaoAtoCanonicoResult {
        if (atoCanonico == null) {
            throw new IllegalArgumentException("atoCanonico é obrigatório");
        }
        if (politica == null) {
            throw new IllegalArgumentException("politica é obrigatória");
        }
        justificativas = List.copyOf(justificativas == null ? List.of() : justificativas);
        if (hashResolucao == null || hashResolucao.isBlank()) {
            throw new IllegalArgumentException("hashResolucao é obrigatório");
        }
        hashResolucao = hashResolucao.trim();
    }
}
