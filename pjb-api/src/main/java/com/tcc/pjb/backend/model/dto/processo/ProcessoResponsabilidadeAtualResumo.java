package com.tcc.pjb.backend.model.dto.processo;

import java.time.LocalDateTime;

public record ProcessoResponsabilidadeAtualResumo(
        String responsabilidadeCode,
        String responsabilidadeTitle,
        String responsavelLabel,
        String responsavelRole,
        String filaCode,
        long tarefasAbertas,
        long tarefasBloqueantes,
        LocalDateTime proximoPrazoEm,
        LocalDateTime atualizadoEm,
        String mensagem
) {
}
