package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.service.processo.ProcessoObservabilidadeAcessoService;

final class PjbAuthorizationAuditFacade {

    private final AuditLedgerService auditLedgerService;
    private final ProcessoObservabilidadeAcessoService processoObservabilidadeAcessoService;
    private final PjbAuthorizationTrailRegistry trailRegistry;
    private final PjbAuthorizationTrailReadModelService trailReadModelService;
    private final PjbAuthorizationTrailAnalyticsRefreshQueueService refreshQueueService;

    PjbAuthorizationAuditFacade(AuditLedgerService auditLedgerService,
                                ProcessoObservabilidadeAcessoService processoObservabilidadeAcessoService,
                                PjbAuthorizationTrailRegistry trailRegistry,
                                PjbAuthorizationTrailReadModelService trailReadModelService,
                                PjbAuthorizationTrailAnalyticsRefreshQueueService refreshQueueService) {
        this.auditLedgerService = auditLedgerService;
        this.processoObservabilidadeAcessoService = processoObservabilidadeAcessoService;
        this.trailRegistry = trailRegistry;
        this.trailReadModelService = trailReadModelService;
        this.refreshQueueService = refreshQueueService;
    }

    void registerDecision(PjbAuthorizationEvaluation evaluation) {
        PjbAuthorizationDecisionTrail trail = evaluation == null ? null : evaluation.trail();
        if (trail == null) {
            return;
        }
        if (appendOnce(trail.auditEventCode(), trail.resourceType(), trail.resourceId(), trail.payloadHash(), trail.auditDescription())) {
            trailRegistry.register(trail);
            PjbAuthorizationTrailSnapshot persistedSnapshot = trailReadModelService.materializeReturningSnapshot(trail);
            if (persistedSnapshot != null) {
                refreshQueueService.enqueueSnapshot(persistedSnapshot);
            }
        }
    }

    void registerGrantedReadVotos(Processo processo) {
        appendProcessAudit("READ_VOTOS", processo);
    }

    void registerGrantedReadProcesso(Processo processo) {
        appendProcessAudit("READ_PROCESSO", processo);
        processoObservabilidadeAcessoService.registrarLeitura(processo);
    }

    void registerGrantedWriteProcesso(Processo processo) {
        appendProcessAudit("WRITE_PROCESSO", processo);
    }

    void registerGrantedReadDocumento(DocumentoProcessual documento) {
        if (documento == null || documento.getId() == null) {
            return;
        }
        appendOnce("READ_DOCUMENTO", "DOCUMENTO", documento.getId().toString(), null, "");
    }

    private void appendProcessAudit(String action, Processo processo) {
        String resourceId = resolveProcessoResourceId(processo);
        if (resourceId == null) {
            return;
        }
        appendOnce(action, "PROCESSO", resourceId, null, "");
    }

    private boolean appendOnce(String action, String resourceType, String resourceId, String payloadHash, String description) {
        String key = action + ':' + resourceType + ':' + resourceId + ':' + payloadHash;
        if (RequestContext.markOnce(key)) {
            auditLedgerService.appendSafely(action, resourceType, resourceId, payloadHash, description == null ? "" : description);
            return true;
        }
        return false;
    }

    private String resolveProcessoResourceId(Processo processo) {
        if (processo == null) {
            return null;
        }
        if (processo.getNumeroUnificado() != null && !processo.getNumeroUnificado().isBlank()) {
            return processo.getNumeroUnificado();
        }
        if (processo.getNumeroProcesso() != null && !processo.getNumeroProcesso().isBlank()) {
            return processo.getNumeroProcesso();
        }
        return processo.getId() != null ? String.valueOf(processo.getId()) : null;
    }
}
