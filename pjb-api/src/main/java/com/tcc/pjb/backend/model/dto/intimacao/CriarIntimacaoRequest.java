package com.tcc.pjb.backend.model.dto.intimacao;

import com.tcc.pjb.backend.model.entity.enums.TipoDestinatarioIntimacao;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CriarIntimacaoRequest(
        @NotBlank @Size(max = 255) String destinatarioNome,
        @NotNull TipoDestinatarioIntimacao destinatarioTipo,
        @Size(max = 20) String destinatarioOab,
        @Email @Size(max = 255) String destinatarioEmail,
        @NotBlank @Size(max = 50) String canal,
        Instant prazoCiencia
) {
}
