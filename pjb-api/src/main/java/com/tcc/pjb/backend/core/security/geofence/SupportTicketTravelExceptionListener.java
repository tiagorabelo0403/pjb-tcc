package com.tcc.pjb.backend.core.security.geofence;

import com.tcc.pjb.backend.configs.datasource.ExecucaoEmContextoDeSistema;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.modules.suporte.entity.SupportTicketCategoria;
import com.tcc.pjb.backend.modules.suporte.event.SupportTicketResolvedEvent;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SupportTicketTravelExceptionListener {

    private static final Logger log = LoggerFactory.getLogger(SupportTicketTravelExceptionListener.class);

    private final JudgeTravelExceptionRepository repository;
    private final ExecucaoEmContextoDeSistema execucaoEmContextoDeSistema;
    private final AuditLedgerService auditService;

    public SupportTicketTravelExceptionListener(JudgeTravelExceptionRepository repository,
                                                ExecucaoEmContextoDeSistema execucaoEmContextoDeSistema,
                                                AuditLedgerService auditService) {
        this.repository = Objects.requireNonNull(repository);
        this.execucaoEmContextoDeSistema = Objects.requireNonNull(execucaoEmContextoDeSistema);
        this.auditService = Objects.requireNonNull(auditService);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoResolverChamado(SupportTicketResolvedEvent evento) {
        if (evento.categoria() != SupportTicketCategoria.EXCECAO_VIAGEM_CARREIRA_JURIDICA || !evento.aprovado()) {
            return;
        }
        try {
            JudgeTravelException excecao = execucaoEmContextoDeSistema.emTransacaoNova(() -> repository.save(JudgeTravelException.builder()
                    .usuarioId(evento.abertoPorId())
                    .ufOuPaisDestino(evento.viagemUfOuPaisDestino())
                    .dataInicio(evento.viagemDataInicio())
                    .dataFim(evento.viagemDataFim())
                    .ticketOrigemId(evento.ticketId())
                    .criadoEm(Instant.now())
                    .build()));
            auditService.appendSafely("GEOFENCE_EXCECAO_VIAGEM_CONCEDIDA", "JUDGE_TRAVEL_EXCEPTION", String.valueOf(excecao.getId()));
        } catch (RuntimeException falha) {
            log.error("Excecao de viagem aprovada nao foi gravada. ticket={} usuario={}", evento.ticketId(), evento.abertoPorId(), falha);
            auditService.appendSafely("GEOFENCE_EXCECAO_VIAGEM_NAO_GRAVADA", "SUPPORT_TICKET", String.valueOf(evento.ticketId()));
        }
    }
}
