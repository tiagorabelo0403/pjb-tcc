package com.tcc.pjb.backend.core.judicial.sobrestamento;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoAuditEntryView;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoAuditHealthView;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoBudgetView;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoConsistencyView;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoDecisionAuditView;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoOwnerView;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoProjectionAuditView;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoProjectionHealthQuery;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoProjectionHealthResult;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaProjection;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoRetomadaCommand;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoRetomadaHealthView;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoRetomadaResult;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoSignalView;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoStatusEnvelopeView;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoStatusHealthView;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaAuditView;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaCommand;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaCompatibilidadeSnapshot;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaConsultaCommand;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaConsultaResult;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaDecisionView;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaHealthQuery;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaHealthResult;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaHealthView;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaResult;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaStatusQuery;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaStatusView;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaTimelineResult;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaWindowHealthView;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaWindowQuery;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaWindowResult;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTimelineAuditView;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTimelineHealthView;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoWindowAuditView;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoWindowHealthQuery;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoWindowHealthResult;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoWindowView;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SobrestamentoApplicationService {

    private final SobrestamentoTemaService service;
    private final AuditLedgerService auditLedgerService;

    public SobrestamentoApplicationService(SobrestamentoTemaService service,
                                           AuditLedgerService auditLedgerService) {
        this.service = Objects.requireNonNull(service);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional
    public SobrestamentoTemaResult sobrestar(String codigoTema) {
        SobrestamentoTemaResult result = service.sobrestar(new SobrestamentoTemaCommand(codigoTema));
        auditLedgerService.appendSafely("SOBRESTAMENTO_MANUAL_RUN", "TEMA", codigoTema, "total=" + result.totalAfetado());
        return result;
    }

    @Transactional
    public SobrestamentoRetomadaResult retomar(String codigoTema, String resultado) {
        SobrestamentoRetomadaResult result = service.retomar(new SobrestamentoRetomadaCommand(codigoTema, resultado));
        auditLedgerService.appendSafely("SOBRESTAMENTO_RETOMADA_MANUAL", "TEMA", codigoTema, "resultado=" + resultado + " total=" + result.totalRetomado());
        return result;
    }

    @Transactional(readOnly = true)
    public SobrestamentoTemaConsultaResult consulta(String codigoTema) {
        return service.consultar(new SobrestamentoTemaConsultaCommand(codigoTema));
    }

    @Transactional(readOnly = true)
    public SobrestamentoTemaStatusView status(String codigoTema) {
        return service.status(new SobrestamentoTemaStatusQuery(codigoTema, null, null));
    }

    @Transactional(readOnly = true)
    public SobrestamentoTemaHealthResult health(String codigoTema) {
        SobrestamentoTemaHealthResult result = service.health(new SobrestamentoTemaHealthQuery(codigoTema));
        auditLedgerService.appendSafely("SOBRESTAMENTO_HEALTH_QUERY", "TEMA", codigoTema, "pendentes=" + result.pendentes());
        return result;
    }

    @Transactional(readOnly = true)
    public SobrestamentoTemaTimelineResult timeline(String codigoTema) {
        SobrestamentoTemaTimelineResult result = service.timeline(codigoTema);
        auditLedgerService.appendSafely("SOBRESTAMENTO_TIMELINE_QUERY", "TEMA", codigoTema, "eventos=" + result.entries().size());
        return result;
    }

    @Transactional(readOnly = true)
    public SobrestamentoConsistencyView consistency(String codigoTema) {
        return service.consistencyView(codigoTema);
    }

    @Transactional(readOnly = true)
    public SobrestamentoBudgetView budget(String codigoTema) {
        return service.budgetView(codigoTema);
    }

    @Transactional(readOnly = true)
    public SobrestamentoTemaProjection projection(String codigoTema) {
        return service.projection(codigoTema);
    }

    @Transactional(readOnly = true)
    public SobrestamentoTemaDecisionView decision(String codigoTema, String classeTpuCodigo) {
        return service.decisionView(codigoTema, classeTpuCodigo);
    }

    @Transactional(readOnly = true)
    public SobrestamentoTemaCompatibilidadeSnapshot compatibilidade(Long processoId, String codigoTema) {
        return service.compatibilidade(processoId, codigoTema);
    }

    @Transactional(readOnly = true)
    public SobrestamentoTemaWindowResult window(String codigoTema) {
        return service.window(new SobrestamentoTemaWindowQuery(codigoTema));
    }

    @Transactional(readOnly = true)
    public SobrestamentoWindowHealthResult windowHealth(String codigoTema, String resultado) {
        return service.windowHealth(new SobrestamentoWindowHealthQuery(codigoTema, resultado));
    }

    @Transactional(readOnly = true)
    public SobrestamentoTemaHealthView healthView(String codigoTema) {
        return service.healthView(codigoTema);
    }

    @Transactional(readOnly = true)
    public SobrestamentoRetomadaHealthView retomadaHealth(String codigoTema, String resultado) {
        return service.retomadaHealthView(codigoTema, resultado);
    }

    @Transactional(readOnly = true)
    public SobrestamentoStatusHealthView statusHealth(String codigoTema) {
        return service.statusHealthView(codigoTema);
    }

    @Transactional(readOnly = true)
    public SobrestamentoTemaWindowHealthView windowHealthView(String codigoTema) {
        return service.windowHealthView(codigoTema);
    }

    @Transactional(readOnly = true)
    public SobrestamentoAuditHealthView auditHealth(String codigoTema) {
        return service.auditHealthView(codigoTema);
    }

    @Transactional(readOnly = true)
    public SobrestamentoTimelineHealthView timelineHealth(String codigoTema) {
        return service.timelineHealthView(codigoTema);
    }

    @Transactional(readOnly = true)
    public SobrestamentoStatusEnvelopeView envelope(String codigoTema) {
        return service.statusEnvelopeView(codigoTema);
    }

    @Transactional(readOnly = true)
    public SobrestamentoSignalView signal(String codigoTema) {
        return service.signalView(codigoTema);
    }

    @Transactional(readOnly = true)
    public SobrestamentoOwnerView owner(String codigoTema) {
        return service.ownerView(codigoTema);
    }

    @Transactional(readOnly = true)
    public SobrestamentoWindowView windowView(String codigoTema) {
        return service.windowView(codigoTema);
    }

    @Transactional(readOnly = true)
    public SobrestamentoAuditEntryView auditEntry(String codigoTema) {
        return service.auditEntryView(codigoTema);
    }

    @Transactional(readOnly = true)
    public SobrestamentoTemaAuditView audit(String codigoTema, String evento) {
        return service.auditView(codigoTema, evento);
    }

    @Transactional(readOnly = true)
    public SobrestamentoTimelineAuditView timelineAudit(String codigoTema) {
        return service.timelineAuditView(codigoTema);
    }

    @Transactional(readOnly = true)
    public SobrestamentoWindowAuditView windowAudit(String codigoTema) {
        return service.windowAuditView(codigoTema);
    }

    @Transactional(readOnly = true)
    public SobrestamentoDecisionAuditView decisionAudit(String codigoTema, String classeTpuCodigo) {
        return service.decisionAuditView(codigoTema, classeTpuCodigo);
    }

    @Transactional(readOnly = true)
    public SobrestamentoProjectionAuditView projectionAudit(String codigoTema) {
        return service.projectionAuditView(codigoTema);
    }

    @Transactional(readOnly = true)
    public SobrestamentoProjectionHealthResult projectionHealth(String codigoTema) {
        return service.projectionHealth(new SobrestamentoProjectionHealthQuery(codigoTema, "SOBRESTAMENTO", 1));
    }
}
