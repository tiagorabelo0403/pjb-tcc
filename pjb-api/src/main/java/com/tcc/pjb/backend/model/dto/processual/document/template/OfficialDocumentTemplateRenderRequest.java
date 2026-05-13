package com.tcc.pjb.backend.model.dto.processual.document.template;

import java.util.Map;
import jakarta.validation.constraints.NotNull;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;

public record OfficialDocumentTemplateRenderRequest(
        @NotNull Long processoId,
        @NotNull TemplateDocumentoOficial template,
        String tituloCustomizado,
        Map<String, String> variaveis,
        Boolean persistir,
        Boolean selarCadeiaConfianca) {
}
