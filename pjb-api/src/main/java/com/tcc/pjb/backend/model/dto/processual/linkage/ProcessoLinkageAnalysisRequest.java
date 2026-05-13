package com.tcc.pjb.backend.model.dto.processual.linkage;

import java.util.List;
import jakarta.validation.constraints.NotNull;

public record ProcessoLinkageAnalysisRequest(
        @NotNull Long processoId,
        List<Long> processoRelacionadoIds,
        Boolean incluirMesmoDocumento,
        Boolean incluirMesmaClasseAssunto,
        Boolean consolidarSinalizacao) {
}
