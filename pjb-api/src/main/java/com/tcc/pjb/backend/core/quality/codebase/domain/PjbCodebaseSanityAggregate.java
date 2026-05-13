package com.tcc.pjb.backend.core.quality.codebase.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PjbCodebaseSanityAggregate(
        boolean disponivel,
        boolean limpo,
        int arquivosEscaneados,
        int fqnsDuplicados,
        int importsInternosQuebrados,
        int virtualThreadsDiretas,
        List<String> diretoriosOrfaos,
        List<String> rootsEscaneadas,
        List<PjbCodebaseSanityIssue> issues,
        Instant geradoEm
) {
    public PjbCodebaseSanityAggregate {
        diretoriosOrfaos = diretoriosOrfaos == null ? List.of() : List.copyOf(diretoriosOrfaos);
        rootsEscaneadas = rootsEscaneadas == null ? List.of() : List.copyOf(rootsEscaneadas);
        issues = issues == null ? List.of() : List.copyOf(issues);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }

    public int score() {
        int penalty = fqnsDuplicados * 35 + importsInternosQuebrados * 20 + virtualThreadsDiretas * 15 + diretoriosOrfaos.size() * 5 + issues.size() * 2;
        return Math.max(0, 100 - penalty);
    }

    public String resumo() {
        return disponivel
                ? (limpo ? "codebase_integrado" : "codebase_com_pendencias")
                : "codebase_indisponivel_no_runtime";
    }
}
