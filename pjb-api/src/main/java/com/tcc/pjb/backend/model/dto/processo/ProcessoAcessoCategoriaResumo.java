package com.tcc.pjb.backend.model.dto.processo;

import java.time.LocalDateTime;

public record ProcessoAcessoCategoriaResumo(
        String categoriaCode,
        String categoriaTitle,
        LocalDateTime ultimoAcessoEm,
        String ultimoAtorLabel,
        String ultimoAtorRole,
        long leitoresAtivos,
        long totalConsultas,
        boolean stepUpSatisfeito,
        String mensagem
) {
}
