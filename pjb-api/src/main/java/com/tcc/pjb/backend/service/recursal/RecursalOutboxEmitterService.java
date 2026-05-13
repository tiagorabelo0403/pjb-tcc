package com.tcc.pjb.backend.service.recursal;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.kernel.process.events.ProcessEventAppendedEvent;
import com.tcc.pjb.backend.core.kernel.process.ProcessEventType;
import com.tcc.pjb.backend.core.kernel.recursal.model.CanonicalFact;
import com.tcc.pjb.backend.core.kernel.recursal.plan.RecursalPlan;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalGraphResponse;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;

@Service
public class RecursalOutboxEmitterService {

  public static final String EVT_RECURSAL_GRAPH_CHANGED = "pjb.recursal.graph.changed";

  private final OutboxPublisher outbox;

  public RecursalOutboxEmitterService(OutboxPublisher outbox) {
    this.outbox = Objects.requireNonNull(outbox);
  }

  public void emitGraphChanged(Long processoId, CanonicalFact fact, RecursalPlan plan, RecursalGraphResponse graph) {
    if (processoId == null) {
      return;
    }
    Map<String, Object> payload = new HashMap<>();
    payload.put("processId", processoId);
    payload.put("eventType", EVT_RECURSAL_GRAPH_CHANGED);
    payload.put("at", Instant.now().toString());
    if (fact != null) {
      payload.put("factId", fact.factId());
      payload.put("dedupKey", fact.dedupKey());
      payload.put("factType", fact.type() != null ? fact.type().name() : null);
      payload.put("sourceSystem", fact.sourceSystem() != null ? fact.sourceSystem().name() : null);
      payload.put("sourceProceedingNumber", fact.sourceProceedingNumber());
      payload.put("observedAt", fact.observedAt() != null ? fact.observedAt().toString() : null);
    }
    if (plan != null) {
      payload.put("proceedingCount", plan.proceedings() != null ? plan.proceedings().size() : 0);
      payload.put("edgePlanCount", plan.edges() != null ? plan.edges().size() : 0);
      payload.put("syncDirectiveCount", plan.sync() != null ? plan.sync().size() : 0);
      payload.put("workItemDirectiveCount", plan.workItems() != null ? plan.workItems().size() : 0);
      payload.put("noteCount", plan.notes() != null ? plan.notes().size() : 0);
    }
    if (graph != null) {
      payload.put("caseFileId", graph.caseFileId());
      payload.put("anchorProceedingKey", graph.anchorProceedingKey());
      payload.put("edgeCount", graph.edges() != null ? graph.edges().size() : 0);
      payload.put("nodeCount", graph.nodes() != null ? graph.nodes().size() : 0);
      if (graph.summary() != null) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalNodes", graph.summary().totalNodes());
        summary.put("totalEdges", graph.summary().totalEdges());
        summary.put("predictedNodes", graph.summary().predictedNodes());
        summary.put("activeNodes", graph.summary().activeNodes());
        summary.put("reconciledNodes", graph.summary().reconciledNodes());
        summary.put("maxInstance", graph.summary().maxInstance() != null ? graph.summary().maxInstance().name() : null);
        payload.put("summary", summary);
      }
    }

    Map<String, Object> headers = new HashMap<>();
    headers.put("source", "recursal_intelligence");
    headers.put("processId", String.valueOf(processoId));

    String dedup = fact != null && fact.dedupKey() != null && !fact.dedupKey().isBlank()
        ? fact.dedupKey()
        : "recursal:" + processoId + ":" + Instant.now().toEpochMilli();

    outbox.enqueue(
        "recursal:" + processoId,
        EVT_RECURSAL_GRAPH_CHANGED,
        payload,
        headers,
        dedup,
        "Processo",
        String.valueOf(processoId)
    );
  }

  @EventListener
  public void onProcessEvent(ProcessEventAppendedEvent ev) {
    if (ev == null) {
      return;
    }
    String type = ev.eventType();
    if (!ProcessEventType.RECURSO_INTERPOSTO.name().equals(type)
        && !ProcessEventType.RECURSO_JULGADO.name().equals(type)) {
      return;
    }

    Map<String, Object> payload = new HashMap<>();
    payload.put("processId", ev.processoId());
    payload.put("eventType", type);
    payload.put("seq", ev.seq());
    payload.put("createdAt", ev.createdAt() != null ? ev.createdAt().toString() : null);
    payload.put("at", Instant.now().toString());

    Map<String, Object> headers = new HashMap<>();
    headers.put("source", "kernel");
    headers.put("processId", String.valueOf(ev.processoId()));

    outbox.enqueue(
        "recursal:" + ev.processoId(),
        EVT_RECURSAL_GRAPH_CHANGED,
        payload,
        headers,
        "recursal:" + ev.processoId() + ":" + ev.seq(),
        "Processo",
        String.valueOf(ev.processoId())
    );
  }
}
