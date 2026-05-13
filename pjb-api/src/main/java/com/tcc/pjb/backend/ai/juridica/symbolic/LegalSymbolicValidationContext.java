package com.tcc.pjb.backend.ai.juridica.symbolic;

import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationRequest;
import java.util.Locale;
import java.util.Map;

public record LegalSymbolicValidationContext(
        String texto,
        String ramo,
        String rito,
        String classe,
        String objetivo,
        String sigilo,
        Map<String, Object> filtros
) {
    public LegalSymbolicValidationContext {
        filtros = filtros == null ? Map.of() : Map.copyOf(filtros);
    }

    public static LegalSymbolicValidationContext from(LegalValidationRequest request) {
        if (request == null) {
            return new LegalSymbolicValidationContext(null, null, null, null, null, null, Map.of());
        }
        return new LegalSymbolicValidationContext(
                request.texto(),
                request.ramo(),
                request.rito(),
                request.classe(),
                request.objetivo(),
                request.sigilo(),
                request.filtros()
        );
    }

    public String normalizedText() {
        return normalize(texto);
    }

    public String normalizedRamo() {
        return normalize(ramo);
    }

    public String normalizedRito() {
        return normalize(rito);
    }

    public String normalizedClasse() {
        return normalize(classe);
    }

    public String normalizedObjetivo() {
        return normalize(objetivo);
    }

    public String normalizedSigilo() {
        return normalize(sigilo);
    }

    public boolean textContains(String... markers) {
        return contains(normalizedText(), markers);
    }

    public boolean objectiveContains(String... markers) {
        return contains(normalizedObjetivo(), markers);
    }

    public boolean ramoContains(String... markers) {
        return contains(normalizedRamo(), markers);
    }

    public boolean ritoContains(String... markers) {
        return contains(normalizedRito(), markers);
    }

    public boolean classeContains(String... markers) {
        return contains(normalizedClasse(), markers);
    }

    public boolean sigiloContains(String... markers) {
        return contains(normalizedSigilo(), markers);
    }

    private boolean contains(String value, String... markers) {
        if (value.isBlank() || markers == null || markers.length == 0) {
            return false;
        }
        for (String marker : markers) {
            if (marker != null && !marker.isBlank() && value.contains(marker.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
