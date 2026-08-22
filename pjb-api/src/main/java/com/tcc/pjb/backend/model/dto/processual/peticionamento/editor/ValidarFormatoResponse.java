package com.tcc.pjb.backend.model.dto.processual.peticionamento.editor;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record ValidarFormatoResponse(
        JsonNode documentoSanitizado,
        boolean alterado,
        List<String> remocoes
) {
}
