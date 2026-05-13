package com.tcc.pjb.backend.core.kernel.advisory;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public final class NegotiationLanguageHeuristics {

    private static final List<String> FRICTION_MARKERS = List.of(
            "sem acordo",
            "sem possibilidade de acordo",
            "sem viabilidade de acordo",
            "nao ha acordo",
            "nao haverá acordo",
            "nao havera acordo",
            "nao aceito",
            "nao aceitamos",
            "recusa",
            "recusamos",
            "impasse",
            "inaceit",
            "inviavel",
            "desproporcional",
            "acordo inviavel",
            "acordo impossivel",
            "impossibilidade de acordo",
            "sem composicao",
            "sem composição"
    );

    private NegotiationLanguageHeuristics() {
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        return normalized.replace('’', '\'').trim();
    }

    public static boolean containsSettlementFriction(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return false;
        }
        return FRICTION_MARKERS.stream().anyMatch(normalized::contains);
    }

    public static boolean containsPositiveSettlementSignal(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank() || containsSettlementFriction(normalized)) {
            return false;
        }
        return normalized.contains("acordo")
                || normalized.contains("composicao")
                || normalized.contains("consenso")
                || normalized.contains("converg")
                || normalized.contains("aceitamos")
                || normalized.contains("aceito")
                || normalized.contains("concord")
                || normalized.contains("fechar");
    }
}
