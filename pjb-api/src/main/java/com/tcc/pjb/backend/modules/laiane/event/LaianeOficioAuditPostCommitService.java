package com.tcc.pjb.backend.modules.laiane.event;

import com.tcc.pjb.backend.modules.auditoria.AuditoriaInteligenteService;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class LaianeOficioAuditPostCommitService {

    private final AuditoriaInteligenteService auditoria;

    public LaianeOficioAuditPostCommitService(AuditoriaInteligenteService auditoria) {
        this.auditoria = Objects.requireNonNull(auditoria);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @PjbTransactionalBudget(operation = "laiane.oficio.audit.post-commit", maxMillis = 500, critical = false)
    public void on(LaianeOficioAuditEvent event) {
        if (event == null || event.acao() == null || event.referenciaId() == null) return;
        auditoria.registrarEventoImutavelJustificado(
                event.acao(),
                event.referenciaId(),
                event.detalhes(),
                event.justificativa()
        );
    }
}
