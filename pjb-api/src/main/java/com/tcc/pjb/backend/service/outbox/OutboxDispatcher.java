package com.tcc.pjb.backend.service.outbox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.outbox.OutboxEvent;
import com.tcc.pjb.backend.service.cidadao.dashboard.CidadaoDashboardOutboxHandler;
import com.tcc.pjb.backend.service.secretariat.live.SecretariatLiveHub;
import com.tcc.pjb.backend.service.ui.accessibility.live.UiAccessibilityLiveHub;
import com.tcc.pjb.backend.service.ui.live.UiHistoryLiveHub;
import com.tcc.pjb.backend.service.ui.presentation.live.UiPresentationLiveHub;

@Component
public class OutboxDispatcher {

  private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

  private final SecretariatLiveHub secretariatLiveHub;
  private final UiHistoryLiveHub uiHistoryLiveHub;
  private final UiAccessibilityLiveHub uiAccessibilityLiveHub;
  private final UiPresentationLiveHub uiPresentationLiveHub;
  private final CidadaoDashboardOutboxHandler cidadaoDashboardOutboxHandler;
  private final ObjectMapper mapper;
  private final ApplicationEventPublisher publisher;
  private final boolean strictTypes;

  public OutboxDispatcher(
      SecretariatLiveHub secretariatLiveHub,
      UiHistoryLiveHub uiHistoryLiveHub,
      UiAccessibilityLiveHub uiAccessibilityLiveHub,
      UiPresentationLiveHub uiPresentationLiveHub,
      CidadaoDashboardOutboxHandler cidadaoDashboardOutboxHandler,
      ObjectMapper mapper,
      ApplicationEventPublisher publisher,
      Environment env
  ) {
    this.secretariatLiveHub = Objects.requireNonNull(secretariatLiveHub);
    this.uiHistoryLiveHub = Objects.requireNonNull(uiHistoryLiveHub);
    this.uiAccessibilityLiveHub = Objects.requireNonNull(uiAccessibilityLiveHub);
    this.uiPresentationLiveHub = Objects.requireNonNull(uiPresentationLiveHub);
    this.cidadaoDashboardOutboxHandler = Objects.requireNonNull(cidadaoDashboardOutboxHandler);
    this.mapper = Objects.requireNonNull(mapper);
    this.publisher = Objects.requireNonNull(publisher);
    this.strictTypes = Boolean.parseBoolean(env.getProperty("pjb.outbox.strictTypes", "false"));
  }

  
  public void dispatch(OutboxEvent e) {
    Objects.requireNonNull(e, "outboxEvent");

    if (OutboxPublisher.EVT_SECRETARIAT_LIVE.equals(e.getEventType())) {
      dispatchSecretariatLive(e);
      return;
    }

    if (OutboxPublisher.EVT_UI_HISTORY_LIVE.equals(e.getEventType())) {
      dispatchUiHistoryLive(e);
      return;
    }

    if (OutboxPublisher.EVT_UI_ACCESSIBILITY_LIVE.equals(e.getEventType())) {
      dispatchUiAccessibilityLive(e);
      return;
    }

    if (OutboxPublisher.EVT_UI_PRESENTATION_LIVE.equals(e.getEventType())) {
      dispatchUiPresentationLive(e);
      return;
    }

    if (OutboxPublisher.EVT_CIDADAO_DASHBOARD_REFRESH.equals(e.getEventType())) {
      cidadaoDashboardOutboxHandler.dispatch(e);
      return;
    }

    if (strictTypes) {
      throw new IllegalStateException("No handler for outbox type: " + e.getEventType());
    }

    publisher.publishEvent(new OutboxGenericDispatchedEvent(
        e.getEventType(),
        e.getRoutingKey(),
        e.getPayloadJson(),
        e.getHeadersJson(),
        e.getAggregateType(),
        e.getAggregateId(),
        e.getCreatedAt()
    ));

    log.debug("Outbox generic dispatched type={} id={} routingKey={}", e.getEventType(), e.getId(), e.getRoutingKey());
  }

  private void dispatchSecretariatLive(OutboxEvent e) {
    try {
      Map<String, Object> msg = parseJsonObject(e.getPayloadJson());
      String inboxKey = firstNonBlank(stringValue(msg.get("inboxKey")), e.getRoutingKey());
      if (inboxKey == null) {
        return;
      }
      secretariatLiveHub.enqueueRaw(inboxKey, e.getPayloadJson());
    } catch (Exception ex) {
      throw new IllegalStateException("secretariat live dispatch", ex);
    }
  }

  private void dispatchUiHistoryLive(OutboxEvent e) {
    String topic = resolveUiHistoryTopic(e);
    if (topic == null) {
      return;
    }
    uiHistoryLiveHub.enqueueRaw(topic, e.getPayloadJson());
  }

  private void dispatchUiAccessibilityLive(OutboxEvent e) {
    String topic = resolveGenericLiveTopic(e);
    if (topic == null) {
      return;
    }
    uiAccessibilityLiveHub.enqueueRaw(topic, e.getPayloadJson());
  }

  private void dispatchUiPresentationLive(OutboxEvent e) {
    String topic = resolveGenericLiveTopic(e);
    if (topic == null) {
      return;
    }
    uiPresentationLiveHub.enqueueRaw(topic, e.getPayloadJson());
  }

  private String resolveUiHistoryTopic(OutboxEvent e) {
    String routingKey = trimToNull(e.getRoutingKey());
    if (routingKey != null) {
      return routingKey;
    }
    Map<String, Object> payload = parseJsonObject(e.getPayloadJson());
    String inboxKey = trimToNull(stringValue(payload.get("inboxKey")));
    if (inboxKey != null) {
      return "HIST:INBOX:" + inboxKey;
    }
    Long workItemId = longValue(payload.get("workItemId"));
    if (workItemId != null) {
      return "HIST:WORKITEM:" + workItemId;
    }
    Long processoId = longValue(payload.get("processoId"));
    if (processoId != null) {
      return "HIST:" + processoId;
    }
    return resolveGenericLiveTopic(e);
  }

  private String resolveGenericLiveTopic(OutboxEvent e) {
    String routingKey = trimToNull(e.getRoutingKey());
    if (routingKey != null) {
      return routingKey;
    }
    Map<String, Object> payload = parseJsonObject(e.getPayloadJson());
    return firstNonBlank(stringValue(payload.get("topic")), stringValue(payload.get("routingKey")), extractTopicFromHeaders(e));
  }

  private String extractTopicFromHeaders(OutboxEvent e) {
    Map<String, Object> headers = parseJsonObject(e.getHeadersJson());
    return firstNonBlank(stringValue(headers.get("topic")), stringValue(headers.get("routingKey")));
  }

  private Map<String, Object> parseJsonObject(String rawJson) {
    try {
      if (rawJson == null || rawJson.isBlank()) {
        return Map.of();
      }
      Map<?, ?> parsed = mapper.readValue(rawJson, Map.class);
      LinkedHashMap<String, Object> values = new LinkedHashMap<>();
      if (parsed != null) {
        parsed.forEach((key, value) -> {
          if (key != null) {
            values.put(String.valueOf(key), value);
          }
        });
      }
      return values.isEmpty() ? Map.of() : Map.copyOf(values);
    } catch (Exception ex) {
      return Map.of();
    }
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      String normalized = trimToNull(value);
      if (normalized != null) {
        return normalized;
      }
    }
    return null;
  }

  private static String stringValue(Object value) {
    if (value == null) {
      return null;
    }
    return String.valueOf(value);
  }

  private static Long longValue(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    try {
      return Long.parseLong(String.valueOf(value).trim());
    } catch (Exception ex) {
      return null;
    }
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
