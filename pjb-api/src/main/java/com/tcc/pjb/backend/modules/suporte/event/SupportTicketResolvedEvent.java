package com.tcc.pjb.backend.modules.suporte.event;

import com.tcc.pjb.backend.modules.suporte.entity.SupportTicketCategoria;
import java.time.LocalDate;

public record SupportTicketResolvedEvent(
        Long ticketId,
        SupportTicketCategoria categoria,
        boolean aprovado,
        Long abertoPorId,
        String viagemUfOuPaisDestino,
        LocalDate viagemDataInicio,
        LocalDate viagemDataFim
) {}
