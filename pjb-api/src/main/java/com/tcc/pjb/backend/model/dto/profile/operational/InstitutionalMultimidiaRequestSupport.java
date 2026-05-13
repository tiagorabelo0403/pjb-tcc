package com.tcc.pjb.backend.model.dto.profile.operational;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import java.util.ArrayList;
import java.util.List;

public final class InstitutionalMultimidiaRequestSupport {

    private InstitutionalMultimidiaRequestSupport() {
    }

    public static List<PeticionamentoMediaBlocoRequest> sanitizeMedia(List<PeticionamentoMediaBlocoRequest> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        ArrayList<PeticionamentoMediaBlocoRequest> out = new ArrayList<>();
        for (PeticionamentoMediaBlocoRequest value : values) {
            if (value != null) {
                out.add(value);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    public static List<String> sanitizeStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<>();
        for (String value : values) {
            if (value != null) {
                String normalized = value.trim();
                if (!normalized.isEmpty() && !out.contains(normalized)) {
                    out.add(normalized);
                }
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }
}
