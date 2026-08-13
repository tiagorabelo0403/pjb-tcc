package com.tcc.pjb.backend.service.secretariat.institucional;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.enums.MotivoEnfileiramentoInstitucional;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SecretariaInstitucionalAutoEnfileiramentoListener {

    private static final Logger log = LoggerFactory.getLogger(SecretariaInstitucionalAutoEnfileiramentoListener.class);
    private static final int PRAZO_BASE_DIAS_PARTE_AUTOMATICA = 15;

    private final SecretariaInstitucionalEnfileiramentoService enfileiramentoService;
    private final AuditLedgerService auditService;

    public SecretariaInstitucionalAutoEnfileiramentoListener(SecretariaInstitucionalEnfileiramentoService enfileiramentoService,
                                                              AuditLedgerService auditService) {
        this.enfileiramentoService = Objects.requireNonNull(enfileiramentoService);
        this.auditService = Objects.requireNonNull(auditService);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoComporPoloInstitucional(PoloInstitucionalComposicaoEvent evento) {
        try {
            enfileiramentoService.enfileirar(evento.processoId(), evento.comarca(), evento.tipo(),
                    MotivoEnfileiramentoInstitucional.PARTE_AUTOMATICA, PRAZO_BASE_DIAS_PARTE_AUTOMATICA);
        } catch (Exception falha) {
            log.error("Falha ao enfileirar item institucional apos commit do ajuizamento processo={} tipo={}",
                    evento.processoId(), evento.tipo(), falha);
            auditService.appendSafely("SECRETARIA_INSTITUCIONAL_ENFILEIRAMENTO_FALHA_POS_COMMIT",
                    "PROCESSO " + evento.processoId() + " tipo=" + evento.tipo() + " erro=" + falha.getMessage());
        }
    }
}
