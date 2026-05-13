package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class RecursalEffectsBlueprint {

    private RecursalEffectsBlueprint() {
    }

    public static List<RecursalEfeito> efeitosProvaveis(String recurso, boolean pretendeEfeitoInfringente) {
        String normalized = normalize(recurso);
        if (normalized.isBlank() || normalized.equals("IRRECORRIVEL") || normalized.equals("PODER_RECORRER_BLOQUEADO")) {
            return List.of();
        }
        Set<RecursalEfeito> effects = new LinkedHashSet<>();
        effects.add(RecursalEfeito.DEVOLUTIVO);
        if (normalized.equals("APELACAO")) {
            effects.add(RecursalEfeito.SUSPENSIVO);
            effects.add(RecursalEfeito.SUBSTITUTIVO);
        }
        if (normalized.equals("EMBARGOS_DECLARACAO")) {
            effects.add(RecursalEfeito.EXPANSIVO);
            if (pretendeEfeitoInfringente) {
                effects.add(RecursalEfeito.SUBSTITUTIVO);
            }
        }
        if (normalized.equals("EMBARGOS_DIVERGENCIA")) {
            effects.add(RecursalEfeito.SUBSTITUTIVO);
        }
        return List.copyOf(effects);
    }

    public static List<String> secoesMinimas(String recurso) {
        String normalized = normalize(recurso);
        List<String> sections = new ArrayList<>();
        if (!normalized.isBlank() && !normalized.equals("IRRECORRIVEL") && !normalized.equals("PODER_RECORRER_BLOQUEADO")) {
            sections.add(RecursalFormalSectionLabels.EFEITOS_RECURSAIS);
            sections.add(RecursalFormalSectionLabels.CABIMENTO);
            if (normalized.equals("APELACAO")) {
                sections.add(RecursalFormalSectionLabels.PEDIDO_EFEITO_SUSPENSIVO);
            }
        }
        return List.copyOf(sections);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
