package com.tcc.pjb.backend.core.comunicacao.institucional.integration.infrastructure;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain.InstitutionalExternalDispatch;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain.InstitutionalExternalDispatchResult;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;

abstract class AbstractOutboxInstitutionalExternalAdapter implements InstitutionalExternalAdapter {

    private final OutboxPublisher outboxPublisher;

    protected AbstractOutboxInstitutionalExternalAdapter(OutboxPublisher outboxPublisher) {
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
    }

    @Override
    public InstitutionalExternalDispatchResult dispatch(InstitutionalExternalDispatch dispatch) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("dispatchId", dispatch.dispatchId());
            payload.put("jobId", dispatch.jobId());
            payload.put("expedicaoUuid", dispatch.expedicaoUuid());
            payload.put("processoId", dispatch.processoId());
            payload.put("processoNumero", dispatch.processoNumero());
            payload.put("unidadeCodigo", dispatch.unidadeCodigo());
            payload.put("caixaCodigo", dispatch.caixaCodigo());
            payload.put("destinatarioKind", dispatch.destinatarioKind().name());
            payload.put("papelProcessual", dispatch.papelProcessual().name());
            payload.put("channel", dispatch.channel().name());
            payload.put("provider", dispatch.provider());
            payload.put("requestPayload", dispatch.requestPayload());
            Map<String, Object> headers = new LinkedHashMap<>();
            headers.put("dispatchId", dispatch.dispatchId());
            headers.put("channel", dispatch.channel().name());
            headers.put("provider", dispatch.provider());
            headers.put("aggregateType", "INSTITUTIONAL_EXTERNAL_DISPATCH");
            headers.put("aggregateId", dispatch.dispatchId());
            UUID outboxId = outboxPublisher.enqueueTracked(
                    dispatch.routingKey(),
                    dispatch.eventType(),
                    payload,
                    headers,
                    dispatch.dedupKey(),
                    "INSTITUTIONAL_EXTERNAL_DISPATCH",
                    dispatch.dispatchId()
            );
            return InstitutionalExternalDispatchResult.accepted(
                    outboxId.toString(),
                    "OUTBOX_ACCEPTED",
                    buildResponse(outboxId.toString())
            );
        } catch (IllegalStateException ex) {
            return InstitutionalExternalDispatchResult.transientFailure("OUTBOX_ERROR", ex.getMessage(), buildError(ex));
        } catch (Exception ex) {
            return InstitutionalExternalDispatchResult.terminalFailure("UNEXPECTED_ERROR", ex.getMessage(), buildError(ex));
        }
    }

    protected abstract String buildResponse(String outboxId);

    protected String buildError(Exception ex) {
        return buildJson(Map.of("error", sanitize(ex.getMessage())));
    }

    protected String buildJson(Map<String, String> values) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!first) {
                json.append(',');
            }
            json.append('"').append(escape(entry.getKey())).append('"')
                    .append(':')
                    .append('"').append(escape(entry.getValue())).append('"');
            first = false;
        }
        json.append('}');
        return json.toString();
    }

    protected String sanitize(String value) {
        if (value == null) {
            return "unknown";
        }
        return value.trim().isEmpty() ? "unknown" : value;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
