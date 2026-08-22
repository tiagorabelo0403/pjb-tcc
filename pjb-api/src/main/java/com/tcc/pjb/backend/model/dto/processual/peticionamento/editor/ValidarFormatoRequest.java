package com.tcc.pjb.backend.model.dto.processual.peticionamento.editor;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public record ValidarFormatoRequest(@NotNull JsonNode documento) {
}
