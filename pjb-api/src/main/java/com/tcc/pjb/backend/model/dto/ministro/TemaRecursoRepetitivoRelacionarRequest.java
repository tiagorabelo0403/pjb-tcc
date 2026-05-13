package com.tcc.pjb.backend.model.dto.ministro;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TemaRecursoRepetitivoRelacionarRequest(
        @NotNull List<Long> processoIds
) {
}
