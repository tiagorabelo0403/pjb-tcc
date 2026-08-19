package com.tcc.pjb.backend.modules.suporte.dto;

import com.tcc.pjb.backend.modules.suporte.entity.SupportTicketCategoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AbrirChamadoRequest(
        @NotNull SupportTicketCategoria categoria,
        @NotBlank @Size(max = 255) String assunto,
        @NotBlank String descricao,
        String viagemUfOuPaisDestino,
        LocalDate viagemDataInicio,
        LocalDate viagemDataFim,
        String viagemJustificativa
) {}
