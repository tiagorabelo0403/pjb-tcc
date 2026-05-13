package com.tcc.pjb.backend.integration.mni;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.integration.mni.application.MniRecepcaoService;
import com.tcc.pjb.backend.integration.mni.application.MniRemessaService;
import com.tcc.pjb.backend.integration.mni.domain.MniConsultaRecepcaoCommand;
import com.tcc.pjb.backend.integration.mni.domain.MniConsultaRemessaCommand;
import com.tcc.pjb.backend.integration.mni.domain.MniEndpointHealthView;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoQuery;
import com.tcc.pjb.backend.integration.mni.domain.MniRemessaBatchCommand;
import com.tcc.pjb.backend.integration.mni.domain.MniRemessaHealthQuery;
import com.tcc.pjb.backend.integration.mni.domain.MniRemessaTimelineQuery;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MniApplicationService {

    private final MniRemessaService remessaService;
    private final MniRecepcaoService recepcaoService;
    private final AuditLedgerService auditLedgerService;

    public MniApplicationService(MniRemessaService remessaService,
                                 MniRecepcaoService recepcaoService,
                                 AuditLedgerService auditLedgerService) {
        this.remessaService = Objects.requireNonNull(remessaService);
        this.recepcaoService = Objects.requireNonNull(recepcaoService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional
    public com.tcc.pjb.backend.integration.mni.domain.MniReprocessamentoSummary reprocessar(Integer limit) {
        int effectiveLimit = limit == null || limit <= 0 ? 25 : limit;
        var summary = remessaService.reprocessarPendentes(new MniRemessaBatchCommand(effectiveLimit));
        auditLedgerService.appendSafely("MNI_REPROCESSAMENTO_RUN", "MNI", String.valueOf(effectiveLimit), null,
                "processadas=" + summary.processadas() + " superseded=" + summary.superseded());
        return summary;
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniConsultaRemessaResult remessa(Long remessaId) {
        return remessaService.consultar(new MniConsultaRemessaCommand(requireId(remessaId)));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniRemessaStatusSnapshot remessaStatus(Long remessaId) {
        return remessaService.statusSnapshot(requireId(remessaId));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniRemessaTimelineResult remessaTimeline(Long remessaId) {
        Long requiredId = requireId(remessaId);
        var timeline = remessaService.timeline(new MniRemessaTimelineQuery(requiredId));
        auditLedgerService.appendSafely("MNI_REMESSA_TIMELINE_QUERY", "MNI_REMESSA", String.valueOf(requiredId), null,
                "entries=" + timeline.entries().size());
        return timeline;
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniRemessaHealthResult remessaHealth(Long remessaId) {
        return remessaService.health(new MniRemessaHealthQuery(requireId(remessaId)));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniPayloadView remessaPayload(Long remessaId) {
        return remessaService.payloadView(requireId(remessaId));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniRemessaWindowSnapshot remessaWindow(Long remessaId) {
        return remessaService.windowSnapshot(requireId(remessaId));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniTribunalEndpointView endpoint(String tribunalCodigo) {
        return remessaService.endpointView(normalize(tribunalCodigo, "tribunalCodigo obrigatorio"));
    }

    @Transactional(readOnly = true)
    public MniEndpointHealthView endpointHealth(String tribunalCodigo) {
        String normalized = normalize(tribunalCodigo, "tribunalCodigo obrigatorio");
        var endpoint = remessaService.endpointView(normalized);
        String status = endpoint.endpoint() == null || endpoint.endpoint().isBlank() ? "DEGRADED" : "OK";
        var view = new MniEndpointHealthView(normalized, status, endpoint.endpoint());
        auditLedgerService.appendSafely("MNI_ENDPOINT_HEALTH_QUERY", "MNI_TRIBUNAL", normalized, null, view.summary());
        return view;
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoQueryResult recepcao(Long recepcaoId) {
        return recepcaoService.consultar(new MniRecepcaoQuery(requireId(recepcaoId)));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoStatusSnapshot recepcaoStatus(Long recepcaoId) {
        return recepcaoService.statusSnapshot(requireId(recepcaoId));
    }

    @Transactional(readOnly = true)
    public java.util.List<com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoTimelineEntry> recepcaoTimeline(Long recepcaoId) {
        Long requiredId = requireId(recepcaoId);
        var timeline = recepcaoService.timeline(requiredId);
        auditLedgerService.appendSafely("MNI_RECEPCAO_TIMELINE_QUERY", "MNI_RECEPCAO", String.valueOf(requiredId), null,
                "entries=" + timeline.size());
        return timeline;
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoHealthSnapshot recepcaoHealth(Long recepcaoId) {
        return recepcaoService.healthSnapshot(requireId(recepcaoId));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoEnvelopeView recepcaoEnvelope(Long recepcaoId) {
        return recepcaoService.envelopeView(requireId(recepcaoId));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoFailureResult recepcaoFailure(Long recepcaoId) {
        return recepcaoService.failureResult(requireId(recepcaoId));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniConsultaRecepcaoResult recepcaoConsulta(Long recepcaoId) {
        return recepcaoService.consultar(new MniConsultaRecepcaoCommand(requireId(recepcaoId)));
    }

    private Long requireId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id obrigatorio");
        }
        return id;
    }

    private String normalize(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
