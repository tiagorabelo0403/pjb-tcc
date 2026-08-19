package com.tcc.pjb.backend.modules.suporte.dto;

import com.tcc.pjb.backend.modules.suporte.entity.SupportTicket;
import com.tcc.pjb.backend.modules.suporte.entity.SupportTicketCategoria;
import com.tcc.pjb.backend.modules.suporte.entity.SupportTicketStatus;
import java.time.Instant;
import java.time.LocalDate;

public record SupportTicketResponse(
        Long id,
        Long abertoPorId,
        String abertoPorNome,
        String abertoPorTipoUsuario,
        SupportTicketCategoria categoria,
        String assunto,
        String descricao,
        SupportTicketStatus status,
        Long atendidoPorId,
        String atendidoPorNome,
        String resposta,
        String viagemUfOuPaisDestino,
        LocalDate viagemDataInicio,
        LocalDate viagemDataFim,
        String viagemJustificativa,
        Instant criadoEm,
        Instant atendidoEm,
        Instant resolvidoEm
) {
    public static SupportTicketResponse de(SupportTicket t) {
        return new SupportTicketResponse(
                t.getId(), t.getAbertoPorId(), t.getAbertoPorNome(), t.getAbertoPorTipoUsuario(),
                t.getCategoria(), t.getAssunto(), t.getDescricao(), t.getStatus(),
                t.getAtendidoPorId(), t.getAtendidoPorNome(), t.getResposta(),
                t.getViagemUfOuPaisDestino(), t.getViagemDataInicio(), t.getViagemDataFim(), t.getViagemJustificativa(),
                t.getCriadoEm(), t.getAtendidoEm(), t.getResolvidoEm());
    }
}
