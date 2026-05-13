package com.tcc.pjb.backend.core.processo.sigilo.domain;

import java.util.List;

public record ProcessoSigiloJurisdicaoBridge(
        String esfera,
        String grau,
        String materia,
        String tribunal,
        String unidade,
        String uf,
        String comarca,
        String foro,
        String anelInstitucional,
        boolean admiteSegredoEstado,
        boolean exigeJuizNatural,
        boolean admiteAudienceDelegado,
        List<String> fundamentos
) {
    public ProcessoSigiloJurisdicaoBridge {
        esfera = normalizeOptional(esfera);
        grau = normalizeOptional(grau);
        materia = normalizeOptional(materia);
        tribunal = normalizeOptional(tribunal);
        unidade = normalizeOptional(unidade);
        uf = normalizeOptional(uf);
        comarca = normalizeOptional(comarca);
        foro = normalizeOptional(foro);
        anelInstitucional = anelInstitucional == null || anelInstitucional.isBlank() ? "PADRAO" : anelInstitucional.trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
