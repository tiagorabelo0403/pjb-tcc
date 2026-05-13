package com.tcc.pjb.backend.modules.balcao.dto;

import com.tcc.pjb.backend.modules.balcao.entity.TipoAtendimentoBalcao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AbrirAtendimentoRequest(
        @NotNull TipoAtendimentoBalcao tipo,
        @NotBlank @Size(max = 255) String solicitanteNome,
        @Size(max = 20) String solicitanteOab,
        @Size(max = 2) String solicitanteOabUf,
        @Size(max = 14) String solicitanteCpf,
        @Size(max = 25) String numeroProcesso,
        @Size(max = 120) String vara,
        @Size(max = 2000) String descricao
) {
}
