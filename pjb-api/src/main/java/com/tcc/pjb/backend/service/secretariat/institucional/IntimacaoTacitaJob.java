package com.tcc.pjb.backend.service.secretariat.institucional;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.SecretariaInstitucionalItemRepository;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class IntimacaoTacitaJob {

    private static final int DIAS_PARA_INTIMACAO_TACITA = 10;

    private final SecretariaInstitucionalItemRepository repository;
    private final AuditLedgerService auditService;
    private final Clock clock;
    private final ProcessoRepository processoRepository;
    private final PrazoFatalCalculator prazoFatalCalculator;

    public IntimacaoTacitaJob(SecretariaInstitucionalItemRepository repository,
                              AuditLedgerService auditService,
                              Clock pjbClock,
                              ProcessoRepository processoRepository,
                              PrazoFatalCalculator prazoFatalCalculator) {
        this.repository = Objects.requireNonNull(repository);
        this.auditService = Objects.requireNonNull(auditService);
        this.clock = Objects.requireNonNull(pjbClock, "pjbClock");
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.prazoFatalCalculator = Objects.requireNonNull(prazoFatalCalculator);
    }

    @Scheduled(fixedDelayString = "${pjb.secretaria-institucional.intimacao-tacita.interval:86400000}")
    @Transactional
    @PjbTransactionalBudget(operation = "secretaria.institucional.intimacao-tacita", maxMillis = 1500, critical = true)
    public void processarIntimacoesTacitas() {
        Instant limite = Instant.now(clock).minus(DIAS_PARA_INTIMACAO_TACITA, ChronoUnit.DAYS);
        List<SecretariaInstitucionalItem> vencidos = repository.buscarPendentesSemCienciaAntesDe(limite);
        for (SecretariaInstitucionalItem item : vencidos) {
            Instant marco = Instant.now(clock);
            item.setIntimacaoTacitaEm(marco);
            item.setStatus(StatusSecretariaInstitucionalItem.EM_ANALISE);
            processoRepository.findById(item.getProcessoId())
                    .ifPresent(processo -> item.setPrazoFatal(prazoFatalCalculator.calcular(item, processo, marco)));
            repository.save(item);
            auditService.appendSafely("SECRETARIA_INSTITUCIONAL_INTIMACAO_TACITA", "SECRETARIA_INSTITUCIONAL_ITEM " + item.getId());
        }
    }
}
