package com.tcc.pjb.backend.integration.judicial.financeiro;

import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudBloqueioRequest;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudConsultaCommand;
import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SisbajudApplicationService {

    private final SisbajudBloqueioService bloqueioService;
    private final AuditLedgerService auditLedgerService;

    public SisbajudApplicationService(SisbajudBloqueioService bloqueioService,
                                      AuditLedgerService auditLedgerService) {
        this.bloqueioService = Objects.requireNonNull(bloqueioService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "sisbajud.application.bloqueio.persist", maxMillis = 2500, critical = true)
    public com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudBloqueioResult bloquear(Long processoId,
                                                                                                        String cpfDevedor,
                                                                                                        BigDecimal valor,
                                                                                                        String numeroOficio,
                                                                                                        String authzTrailId,
                                                                                                        boolean delegatedOperation) {
        var result = bloqueioService.solicitarBloqueio(new SisbajudBloqueioRequest(processoId, cpfDevedor, valor, numeroOficio, delegatedOperation), authzTrailId);
        auditLedgerService.appendSafely("SISBAJUD_BLOQUEIO_MANUAL", "PROCESSO", String.valueOf(processoId), null, "success=" + result.success());
        return result;
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "sisbajud.application.consulta.read", maxMillis = 1200, critical = false)
    public com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudConsultaResult consulta(Long operacaoId) {
        return bloqueioService.consultar(new SisbajudConsultaCommand(requireId(operacaoId)));
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "sisbajud.application.snapshot.read", maxMillis = 1200, critical = false)
    public com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudOperacaoSnapshot snapshot(Long operacaoId) {
        return bloqueioService.snapshot(requireId(operacaoId));
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "sisbajud.application.retry.read", maxMillis = 1200, critical = false)
    public com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudRetrySnapshot retry(Long operacaoId) {
        return bloqueioService.retrySnapshot(requireId(operacaoId));
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "sisbajud.application.audit.read", maxMillis = 1200, critical = false)
    public com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudOperacaoAuditSnapshot audit(Long operacaoId) {
        return bloqueioService.auditSnapshot(requireId(operacaoId));
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "sisbajud.application.view.read", maxMillis = 1200, critical = false)
    public com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudOperacaoView view(Long operacaoId) {
        return bloqueioService.view(requireId(operacaoId));
    }

    private Long requireId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("operacaoId obrigatorio");
        }
        return id;
    }
}