package com.tcc.pjb.backend.core.security.geofence;

import com.tcc.pjb.backend.modules.suporte.entity.SupportTicketCategoria;
import com.tcc.pjb.backend.modules.suporte.event.SupportTicketResolvedEvent;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SupportTicketTravelExceptionListener {

    private final JudgeTravelExceptionRepository repository;

    public SupportTicketTravelExceptionListener(JudgeTravelExceptionRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoResolverChamado(SupportTicketResolvedEvent evento) {
        if (evento.categoria() != SupportTicketCategoria.EXCECAO_VIAGEM_MAGISTRATURA || !evento.aprovado()) {
            return;
        }
        repository.save(JudgeTravelException.builder()
                .usuarioId(evento.abertoPorId())
                .ufOuPaisDestino(evento.viagemUfOuPaisDestino())
                .dataInicio(evento.viagemDataInicio())
                .dataFim(evento.viagemDataFim())
                .ticketOrigemId(evento.ticketId())
                .criadoEm(Instant.now())
                .build());
    }
}
