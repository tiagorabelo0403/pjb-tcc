package com.tcc.pjb.backend.model.dto.oficial_justica;

import com.tcc.pjb.backend.core.comunicacao.judicial.RecusaRecebimentoService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RecusaRecebimentoRequest(
        @NotNull Long processoId,
        @NotBlank String destinatarioNome,
        String destinatarioDocumento,
        double latitude,
        double longitude,
        String enderecoConfirmado,
        @NotEmpty List<RecusaRecebimentoService.TipoEvidenciaRecusa> evidencias,
        String fotoHashBase64,
        String biometriaHashOficial,
        String observacoes
) {
}
