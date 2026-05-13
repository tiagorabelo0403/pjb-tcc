package com.tcc.pjb.backend.ai.core;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum IACapability {
    ANALISE_JURIDICA,
    TRIAGEM_PROCESSUAL,
    FUNDAMENTACAO_CONSTITUCIONAL,
    JURISPRUDENCIA_STF_STJ,
    CALCULO_FINANCEIRO,
    DOSIMETRIA_PENAL,
    ANALISE_PROBATORIA,
    RISCO_PROCESSUAL,
    APOIO_DELEGADO,
    APOIO_JUIZ,
    APOIO_MINISTERIO_PUBLICO;

    public static Optional<IACapability> tryParse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String token = raw.trim().toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_')
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Ã', 'A')
                .replace('Â', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ô', 'O')
                .replace('Õ', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C');
        try {
            return Optional.of(IACapability.valueOf(token));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public static Set<IACapability> defaultsFor(IAProfile profile) {
        LinkedHashSet<IACapability> out = new LinkedHashSet<>();
        out.add(ANALISE_JURIDICA);
        out.add(TRIAGEM_PROCESSUAL);
        out.add(RISCO_PROCESSUAL);
        if (profile == null) {
            return Set.copyOf(out);
        }
        if (profile.isMagistratura()) {
            out.add(APOIO_JUIZ);
            out.add(FUNDAMENTACAO_CONSTITUCIONAL);
            out.add(JURISPRUDENCIA_STF_STJ);
            out.add(ANALISE_PROBATORIA);
        }
        if (profile.isAuxiliarJustica()) {
            out.add(ANALISE_PROBATORIA);
        }
        if (profile.isOrgaoJusticaPublica()) {
            out.add(JURISPRUDENCIA_STF_STJ);
            out.add(FUNDAMENTACAO_CONSTITUCIONAL);
        }
        switch (profile) {
            case DELEGADO_ESTADUAL, DELEGADO_FEDERAL -> {
                out.add(APOIO_DELEGADO);
                out.add(ANALISE_PROBATORIA);
            }
            case PROMOTOR, PROMOTOR_ELEITORAL, PROMOTOR_TRABALHISTA, MINISTERIO_PUBLICO, PROCURADOR_GERAL_REPUBLICA -> {
                out.add(APOIO_MINISTERIO_PUBLICO);
                out.add(JURISPRUDENCIA_STF_STJ);
            }
            case CONTADOR_JUDICIAL, PERITO_CONTABIL -> out.add(CALCULO_FINANCEIRO);
            case PERITO_GERAL, PERITO_CRIMINAL, PERITO_DIGITAL, PERITO_MEDICO -> out.add(ANALISE_PROBATORIA);
            default -> {
            }
        }
        return Set.copyOf(out);
    }
}
