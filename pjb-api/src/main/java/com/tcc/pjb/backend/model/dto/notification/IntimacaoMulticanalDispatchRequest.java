package com.tcc.pjb.backend.model.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IntimacaoMulticanalDispatchRequest(@NotBlank String titulo,
                                                 @NotBlank String mensagem,
                                                 String urlAcesso,
                                                 @NotNull Boolean prioridadeAlta) {
}
