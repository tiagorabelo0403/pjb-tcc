package com.tcc.pjb.backend.model.dto.criminal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.List;

public record PoliceNativeCautelarDispatchRequest(
        Long inqueritoId,
        Long processoId,
        @NotBlank String tipoMedida,
        @NotBlank String fundamento,
        @Size(max = 40) List<String> referenciasEvidencia,
        Boolean sigilo,
        String tribunalAlvo,
        Boolean permitirRemessaParceira,
        Boolean prioridadeAlta
) {
    public PoliceNativeCautelarDispatchRequest {
        tipoMedida = sanitize(tipoMedida, "MEDIDA_CAUTELAR");
        fundamento = sanitize(fundamento, "Fundamento operacional não informado");
        referenciasEvidencia = sanitizeStrings(referenciasEvidencia);
        tribunalAlvo = sanitize(tribunalAlvo, "TRIBUNAL_PADRAO");
    }

    public boolean sigiloResolvido() {
        return Boolean.TRUE.equals(sigilo);
    }

    public boolean permitirRemessaParceiraResolvido() {
        return !Boolean.FALSE.equals(permitirRemessaParceira);
    }

    public boolean prioridadeAltaResolvida() {
        return Boolean.TRUE.equals(prioridadeAlta);
    }

    private static String sanitize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static List<String> sanitizeStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            out.add(value.trim());
        }
        return List.copyOf(out);
    }
}
