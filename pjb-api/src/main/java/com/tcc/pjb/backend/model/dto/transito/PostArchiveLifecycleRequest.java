package com.tcc.pjb.backend.model.dto.transito;

import jakarta.validation.constraints.NotNull;

public record PostArchiveLifecycleRequest(
        @NotNull Long processoId,
        boolean reativar,
        String motivo,
        boolean verificarDocumentosNovos,
        boolean verificarMovimentacaoRecente,
        int janelaDias
) {
}
