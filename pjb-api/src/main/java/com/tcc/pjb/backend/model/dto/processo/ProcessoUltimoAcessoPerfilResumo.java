package com.tcc.pjb.backend.model.dto.processo;

import java.time.LocalDateTime;

public record ProcessoUltimoAcessoPerfilResumo(
        String categoriaCode,
        String categoriaTitle,
        LocalDateTime ultimoAcessoEm,
        String ultimoAtorLabel,
        String ultimoAtorRole,
        String canal,
        boolean stepUpSatisfeito,
        String mensagem
) {
}
