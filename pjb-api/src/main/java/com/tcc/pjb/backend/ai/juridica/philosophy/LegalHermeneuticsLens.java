package com.tcc.pjb.backend.ai.juridica.philosophy;

import java.util.List;

public final class LegalHermeneuticsLens {

    private LegalHermeneuticsLens() {}

    public static List<String> defaultLenses() {
        return List.of(
                "interpretação literal/gramatical (ponto de partida)",
                "interpretação sistemática (coerência do ordenamento)",
                "interpretação teleológica/finalística (finalidade da norma)",
                "interpretação conforme a Constituição (princípios e proporcionalidade)",
                "precedentes e estabilidade (distinguishing/overruling quando necessário)",
                "ponderação (adequação, necessidade e proporcionalidade em sentido estrito)"
        );
    }
}
