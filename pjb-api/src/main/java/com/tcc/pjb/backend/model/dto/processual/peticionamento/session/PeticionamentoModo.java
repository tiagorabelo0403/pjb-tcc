package com.tcc.pjb.backend.model.dto.processual.peticionamento.session;

import java.util.Locale;

public enum PeticionamentoModo {
    MANUAL_GUIADO,
    ASSISTIDO_LAIANE,
    HIBRIDO;

    public static PeticionamentoModo fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return HIBRIDO;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        return switch (normalized) {
            case "MANUAL", "MANUAL_GUIADO", "GUIADO" -> MANUAL_GUIADO;
            case "ASSISTIDO", "ASSISTIDO_LAIANE", "LAIANE", "IA" -> ASSISTIDO_LAIANE;
            case "HIBRIDO", "HYBRID" -> HIBRIDO;
            default -> HIBRIDO;
        };
    }

    public boolean includeManual() {
        return this == MANUAL_GUIADO || this == HIBRIDO;
    }

    public boolean includeAssistive() {
        return this == ASSISTIDO_LAIANE || this == HIBRIDO;
    }
}
