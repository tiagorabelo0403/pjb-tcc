package com.tcc.pjb.backend.model.dto.profile.operational;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public record PeritoLaudoRequest(
        @NotBlank String tipoLaudo,
        @NotBlank String conclusao,
        @NotBlank String metodologia,
        @Valid @Size(max = 64) List<PeticionamentoMediaBlocoRequest> midiaInline,
        @Size(max = 96) List<String> provasDocumentais,
        @Size(max = 96) List<String> documentosPessoais,
        @Size(max = 96) List<String> documentosRepresentacao,
        @Size(max = 96) List<String> documentosAnexados,
        Boolean prepararPacoteProtocolo
) {
    public PeritoLaudoRequest {
        midiaInline = sanitizeMedia(midiaInline);
        provasDocumentais = sanitizeStrings(provasDocumentais);
        documentosPessoais = sanitizeStrings(documentosPessoais);
        documentosRepresentacao = sanitizeStrings(documentosRepresentacao);
        documentosAnexados = sanitizeStrings(documentosAnexados);
    }

    public boolean prepararPacoteProtocoloResolvido() {
        return Boolean.TRUE.equals(prepararPacoteProtocolo);
    }

    private static List<PeticionamentoMediaBlocoRequest> sanitizeMedia(List<PeticionamentoMediaBlocoRequest> values) {
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

    private static List<String> sanitizeStrings(List<String> values) {
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
