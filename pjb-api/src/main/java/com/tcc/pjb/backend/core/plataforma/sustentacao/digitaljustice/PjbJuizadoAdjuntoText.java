package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import java.text.Normalizer;
import java.util.Locale;

final class PjbJuizadoAdjuntoText {

    private PjbJuizadoAdjuntoText() {
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return ascii.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", " ").trim();
    }
}
