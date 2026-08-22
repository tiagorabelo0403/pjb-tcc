package com.tcc.pjb.backend.model.dto.processual.peticionamento.editor;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExportarDocxRequest(
        @NotNull JsonNode documento,
        @Size(max = 180) String tituloCaso
) {
}
