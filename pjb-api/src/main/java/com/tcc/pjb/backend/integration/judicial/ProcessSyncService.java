package com.tcc.pjb.backend.integration.judicial;

import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProcessSyncService {

  private static final Logger log = LoggerFactory.getLogger(ProcessSyncService.class);

  public static final String OUTBOX_EVENT_TYPE = "pjb.external.process.event";
  public static final String OUTBOX_SNAPSHOT_TYPE = "pjb.external.process.snapshot";

  private final JudicialConnectorRegistry registry;
  private final ProcessEventNormalizer normalizer;
  private final OutboxPublisher outbox;
  private final JudicialConnectorTelemetryService telemetryService;

  public ProcessSyncService(JudicialConnectorRegistry registry,
                            ProcessEventNormalizer normalizer,
                            OutboxPublisher outbox,
                            JudicialConnectorTelemetryService telemetryService) {
    this.registry = Objects.requireNonNull(registry);
    this.normalizer = Objects.requireNonNull(normalizer);
    this.outbox = Objects.requireNonNull(outbox);
    this.telemetryService = Objects.requireNonNull(telemetryService);
  }

  public Optional<ExternalProcessSnapshot> syncSnapshot(JudicialSystem system, String numeroUnificado) {
    if (numeroUnificado == null || numeroUnificado.isBlank()) {
      return Optional.empty();
    }

    JudicialProcessConnector connector = registry.get(system);
    Optional<ExternalProcessSnapshot> snapshot = connector.fetchSnapshotByNumero(numeroUnificado.trim());
    telemetryService.recordSnapshotResult(system, numeroUnificado.trim(), snapshot.orElse(null));
    snapshot.ifPresent(item -> {
      String dedupKey = "judicial:snapshot:" + connector.system() + ':' + item.numeroUnificado();
      try {
        outbox.enqueue(
            "judicial:snapshot:" + connector.system() + ':' + item.numeroUnificado(),
            OUTBOX_SNAPSHOT_TYPE,
            item,
            snapshotHeaders(item),
            dedupKey,
            "Processo",
            item.numeroUnificado()
        );
      } catch (Exception ex) {
        log.warn("Falha ao enfileirar snapshot externo numero={} system={}: {}", item.numeroUnificado(), connector.system(), ex.getMessage());
      }
    });
    return snapshot;
  }

  public int syncEvents(JudicialSystem system, String numeroUnificado, Instant since) {
    if (numeroUnificado == null || numeroUnificado.isBlank()) {
      return 0;
    }

    JudicialProcessConnector connector = registry.get(system);
    List<ExternalProcessEvent> events = connector.fetchEvents(numeroUnificado.trim(), since);

    int count = 0;
    for (ExternalProcessEvent e : events) {
      NormalizedProcessEvent n = normalizer.normalize(e);
      if (n == null) {
        continue;
      }

      String externalId = safe(e.externalId());
      String dedupKey = "judicial:" + connector.system() + ":" + n.numeroUnificado() + ":" +
          (externalId != null ? externalId : n.occurredAt().toEpochMilli());

      try {
        outbox.enqueue(
            "judicial:" + connector.system() + ":" + n.numeroUnificado(),
            OUTBOX_EVENT_TYPE,
            n,
            headersFor(e),
            dedupKey,
            "Processo",
            n.numeroUnificado()
        );
        count++;
      } catch (Exception ex) {
        log.warn("Falha ao enfileirar evento externo numero={} system={} externalId={}: {}",
            numeroUnificado, connector.system(), externalId, ex.getMessage());
      }
    }
    telemetryService.recordEventsResult(system, numeroUnificado.trim(), since, count);
    return count;
  }

  private static Map<String, Object> snapshotHeaders(ExternalProcessSnapshot snapshot) {
    Map<String, Object> h = new HashMap<>();
    h.put("sourceSystem", snapshot.system() != null ? snapshot.system().name() : null);
    h.put("numeroUnificado", snapshot.numeroUnificado());
    h.put("classeProcessual", snapshot.classeProcessual());
    h.put("assunto", snapshot.assunto());
    h.put("nivelSigilo", snapshot.nivelSigilo() != null ? snapshot.nivelSigilo().name() : null);
    h.put("fetchedAt", snapshot.fetchedAt() != null ? snapshot.fetchedAt().toString() : null);
    return h;
  }

  private static Map<String, Object> headersFor(ExternalProcessEvent e) {
    Map<String, Object> h = new HashMap<>();
    h.put("sourceSystem", e.system() != null ? e.system().name() : null);
    h.put("externalId", e.externalId());
    h.put("externalType", e.type());
    h.put("occurredAt", e.occurredAt() != null ? e.occurredAt().toString() : null);
    h.put("numeroUnificado", e.numeroUnificado());
    return h;
  }

  private static String safe(String s) {
    if (s == null) return null;
    String v = s.trim();
    return v.isBlank() ? null : v;
  }
}
