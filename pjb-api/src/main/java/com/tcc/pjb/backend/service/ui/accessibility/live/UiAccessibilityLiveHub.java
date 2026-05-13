package com.tcc.pjb.backend.service.ui.accessibility.live;

import com.tcc.pjb.backend.configs.live.LiveClusterBus;
import com.tcc.pjb.backend.configs.live.LiveClusterEvent;
import com.tcc.pjb.backend.configs.live.LiveClusterStateStore;
import com.tcc.pjb.backend.configs.live.LiveWindowSupport;
import com.tcc.pjb.backend.service.sse.TooManySseConnectionsException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class UiAccessibilityLiveHub {

  private static final Logger log = LoggerFactory.getLogger(UiAccessibilityLiveHub.class);

  private static final String EVENT_NAME = "ui-accessibility";
  private static final String CLUSTER_NAMESPACE = "ui-accessibility";

  private final int replayBufferSize;
  private final int maxBatchEvents;
  private final int maxPendingBacklog;
  private final long emitterTimeoutMs;
  private final int maxChannels;
  private final int maxSubscribersPerTopic;
  private final int maxChannelsPerFlushCycle;
  private final int maxChannelsPerRefreshCycle;
  private final long idleChannelTtlNanos;
  private final Duration replayTtl;
  private final Duration subscriberTtl;

  private final ExecutorService io;
  private final LiveClusterBus clusterBus;
  private final LiveClusterStateStore clusterStateStore;
  private final ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>();
  private final AtomicInteger flushCursor = new AtomicInteger(0);
  private final AtomicInteger refreshCursor = new AtomicInteger(0);
  private final AtomicInteger heartbeatCursor = new AtomicInteger(0);

  public UiAccessibilityLiveHub(
      @Qualifier("pjbLiveExecutorService") ExecutorService io,
      LiveClusterBus clusterBus,
      LiveClusterStateStore clusterStateStore,
      @Value("${pjb.ui.accessibility.sse.replayBuffer:300}") int replayBufferSize,
      @Value("${pjb.ui.accessibility.sse.maxBatchEvents:200}") int maxBatchEvents,
      @Value("${pjb.ui.accessibility.sse.maxPendingBacklog:2000}") int maxPendingBacklog,
      @Value("${pjb.ui.accessibility.sse.emitterTimeoutMs:1800000}") long emitterTimeoutMs,
      @Value("${pjb.ui.accessibility.sse.maxChannels:4096}") int maxChannels,
      @Value("${pjb.ui.accessibility.sse.maxSubscribersPerTopic:8}") int maxSubscribersPerTopic,
      @Value("${pjb.ui.accessibility.sse.maxChannelsPerFlushCycle:256}") int maxChannelsPerFlushCycle,
      @Value("${pjb.ui.accessibility.sse.maxChannelsPerRefreshCycle:512}") int maxChannelsPerRefreshCycle,
      @Value("${pjb.ui.accessibility.sse.idleChannelTtl:5m}") Duration idleChannelTtl,
      @Value("${pjb.live.cluster.replay-ttl-seconds:900}") long replayTtlSeconds,
      @Value("${pjb.live.cluster.subscriber-ttl-seconds:120}") long subscriberTtlSeconds
  ) {
    this.replayBufferSize = Math.max(50, replayBufferSize);
    this.maxBatchEvents = Math.max(50, maxBatchEvents);
    this.maxPendingBacklog = Math.max(this.maxBatchEvents, maxPendingBacklog);
    this.emitterTimeoutMs = Math.max(60_000L, emitterTimeoutMs);
    this.maxChannels = Math.max(64, maxChannels);
    this.maxSubscribersPerTopic = Math.max(1, maxSubscribersPerTopic);
    this.maxChannelsPerFlushCycle = Math.max(1, maxChannelsPerFlushCycle);
    this.maxChannelsPerRefreshCycle = Math.max(1, maxChannelsPerRefreshCycle);
    this.idleChannelTtlNanos = Objects.requireNonNull(idleChannelTtl, "idleChannelTtl").toNanos();
    this.replayTtl = Duration.ofSeconds(Math.max(120L, replayTtlSeconds));
    this.subscriberTtl = Duration.ofSeconds(Math.max(30L, subscriberTtlSeconds));
    this.io = Objects.requireNonNull(io, "io");
    this.clusterBus = Objects.requireNonNull(clusterBus, "clusterBus");
    this.clusterStateStore = Objects.requireNonNull(clusterStateStore, "clusterStateStore");
    this.clusterBus.registerHandler(CLUSTER_NAMESPACE, this::deliverClusterEvent);
  }

  public SseEmitter register(String topic, String lastEventId) {
    Objects.requireNonNull(topic, "topic");
    Channel ch = registrationChannel(topic);
    SseEmitter emitter = new SseEmitter(emitterTimeoutMs);
    long wantSeq = parseLastEventId(lastEventId).orElse(-1L);
    Subscriber sub = ch.addSubscriber(emitter);
    ch.syncSubscribers();
    emitter.onCompletion(() -> {
      ch.removeSubscriber(sub.id);
      ch.syncSubscribers();
      removeIfIdle(ch);
    });
    emitter.onTimeout(() -> {
      ch.removeSubscriber(sub.id);
      ch.syncSubscribers();
      removeIfIdle(ch);
    });
    emitter.onError(ex -> {
      ch.removeSubscriber(sub.id);
      ch.syncSubscribers();
      removeIfIdle(ch);
    });
    io.execute(() -> {
      try {
        ch.replayTo(sub, wantSeq);
      } catch (Exception ex) {
        ch.removeSubscriber(sub.id);
        ch.syncSubscribers();
        removeIfIdle(ch);
      }
    });
    return emitter;
  }


  public void enqueueRaw(String topic, String json) {
    if (topic == null || topic.isBlank() || json == null || json.isBlank()) {
      return;
    }
    Channel ch = publishChannel(topic);
    if (ch == null) {
      return;
    }
    ch.enqueue(json);
  }

  @Scheduled(fixedDelayString = "${pjb.ui.accessibility.sse.flushMs:200}")
  public void flushPending() {
    LiveWindowSupport.forWindow(channels, flushCursor, maxChannelsPerFlushCycle, ch -> {
      try {
        ch.flushBatches(maxBatchEvents);
      } catch (Exception ex) {
        log.debug("ui-accessibility flush failed: {}", ex.getMessage());
      }
      removeIfIdle(ch);
    });
  }

  @Scheduled(fixedDelayString = "${pjb.live.cluster.subscriber-refresh-ms:30000}")
  public void refreshClusterState() {
    LiveWindowSupport.forWindow(channels, refreshCursor, maxChannelsPerRefreshCycle, ch -> {
      ch.syncSubscribers();
      removeIfIdle(ch);
    });
  }

  public void heartbeat() {
    String hb = "{\"type\":\"HEARTBEAT\",\"at\":\"" + Instant.now().toString() + "\"}";
    LiveWindowSupport.forWindow(channels, heartbeatCursor, maxChannelsPerRefreshCycle, ch -> ch.enqueue(hb));
  }

  private Channel registrationChannel(String topic) {
    Channel existing = channels.get(topic);
    if (existing != null) {
      existing.touch();
      return existing;
    }
    cleanupIdleChannels();
    Channel created = new Channel(topic, maxPendingBacklog);
    Channel prior = channels.putIfAbsent(topic, created);
    Channel result = prior == null ? created : prior;
    if (prior == null && channels.size() > maxChannels) {
      channels.remove(topic, created);
      throw new TooManySseConnectionsException("too many sse");
    }
    result.touch();
    return result;
  }

  private Channel publishChannel(String topic) {
    Channel existing = channels.get(topic);
    if (existing != null) {
      existing.touch();
      return existing;
    }
    cleanupIdleChannels();
    Channel created = new Channel(topic, maxPendingBacklog);
    Channel prior = channels.putIfAbsent(topic, created);
    Channel result = prior == null ? created : prior;
    if (prior == null && channels.size() > maxChannels) {
      channels.remove(topic, created);
      return null;
    }
    result.touch();
    return result;
  }

  private void cleanupIdleChannels() {
    long now = System.nanoTime();
    for (Channel ch : channels.values()) {
      if (ch.isRemovable(now)) {
        ch.syncSubscribers();
        channels.remove(ch.key(), ch);
      }
    }
  }

  private void removeIfIdle(Channel ch) {
    long now = System.nanoTime();
    if (ch.isRemovable(now)) {
      ch.syncSubscribers();
      channels.remove(ch.key(), ch);
    }
  }

  private void deliverClusterEvent(LiveClusterEvent event) {
    if (event == null || event.topic() == null || event.topic().isBlank() || event.payload() == null || event.payload().isBlank() || event.sequence() <= 0) {
      return;
    }
    Channel ch = channels.get(event.topic());
    if (ch == null) {
      return;
    }
    ch.publishReplicated(event.sequence(), event.payload());
  }

  private static Optional<Long> parseLastEventId(String lastEventId) {
    if (lastEventId == null || lastEventId.isBlank()) {
      return Optional.empty();
    }
    String s = lastEventId.trim();
    int idx = s.lastIndexOf(':');
    if (idx >= 0 && idx + 1 < s.length()) {
      s = s.substring(idx + 1);
    }
    try {
      return Optional.of(Long.parseLong(s));
    } catch (Exception ignored) {
      return Optional.empty();
    }
  }

  private final class Channel {
    private final String topic;
    private final int maxPendingBacklog;
    private final AtomicInteger pendingCount = new AtomicInteger(0);
    private final AtomicLong latestSequence = new AtomicLong(0);
    private final AtomicLong lastTouchedNanos = new AtomicLong(System.nanoTime());
    private final ConcurrentHashMap<String, Subscriber> subs = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> pending = new ConcurrentLinkedQueue<>();

    Channel(String topic, int maxPendingBacklog) {
      this.topic = topic;
      this.maxPendingBacklog = maxPendingBacklog;
    }

    String key() {
      return topic;
    }

    void touch() {
      lastTouchedNanos.set(System.nanoTime());
    }

    Subscriber addSubscriber(SseEmitter emitter) {
      while (true) {
        if (subs.size() >= maxSubscribersPerTopic) {
          throw new TooManySseConnectionsException("too many sse");
        }
        String id = UUID.randomUUID().toString();
        Subscriber s = new Subscriber(id, emitter);
        if (subs.putIfAbsent(id, s) == null) {
          touch();
          return s;
        }
      }
    }

    int subscriberCount() {
      return subs.size();
    }

    void removeSubscriber(String id) {
      Subscriber s = subs.remove(id);
      if (s != null) {
        touch();
        s.closeQuiet();
      }
    }

    void syncSubscribers() {
      clusterStateStore.syncSubscriberCount(CLUSTER_NAMESPACE, topic, subs.size(), subscriberTtl);
    }

    boolean isRemovable(long nowNanos) {
      return subs.isEmpty()
          && pendingCount.get() == 0
          && nowNanos - lastTouchedNanos.get() >= idleChannelTtlNanos;
    }

    void enqueue(String json) {
      if (json == null || json.isBlank()) {
        return;
      }
      touch();
      while (pendingCount.get() >= maxPendingBacklog) {
        String dropped = pending.poll();
        if (dropped == null) {
          break;
        }
        pendingCount.decrementAndGet();
      }
      pending.add(json);
      pendingCount.incrementAndGet();
    }

    void flushBatches(int maxBatchEvents) {
      touch();
      if (subs.isEmpty() && !clusterBus.enabled() && !clusterStateStore.distributed()) {
        pending.clear();
        pendingCount.set(0);
        return;
      }
      List<String> slice = new ArrayList<>(Math.min(maxBatchEvents, 64));
      while (slice.size() < maxBatchEvents) {
        String e = pending.poll();
        if (e == null) {
          break;
        }
        pendingCount.decrementAndGet();
        slice.add(e);
      }
      if (slice.isEmpty()) {
        return;
      }
      String batchJson = buildBatchJson(topic, slice);
      publishLocal(batchJson);
    }

    void replayTo(Subscriber sub, long wantSeq) throws IOException {
      touch();
      long latest = Math.max(latestSequence.get(), clusterStateStore.latestSequence(CLUSTER_NAMESPACE, topic));
      if (wantSeq >= 0 && latest - wantSeq > replayBufferSize) {
        send(sub, "{\"type\":\"RESYNC_REQUIRED\",\"at\":\"" + Instant.now().toString() + "\"}", latest);
        return;
      }
      List<LiveClusterStateStore.ReplayEntry> entries = clusterStateStore.replayAfter(CLUSTER_NAMESPACE, topic, wantSeq, replayBufferSize);
      for (LiveClusterStateStore.ReplayEntry entry : entries) {
        latestSequence.accumulateAndGet(entry.sequence(), Math::max);
        send(sub, entry.payload(), entry.sequence());
      }
    }

    void publishReplicated(long sequence, String json) {
      if (sequence <= 0 || json == null || json.isBlank()) {
        return;
      }
      touch();
      long seen = latestSequence.get();
      if (sequence <= seen) {
        return;
      }
      latestSequence.accumulateAndGet(sequence, Math::max);
      for (Subscriber sub : subs.values()) {
        try {
          send(sub, json, sequence);
        } catch (Exception ex) {
          removeSubscriber(sub.id);
        }
      }
      syncSubscribers();
    }

    private void publishLocal(String json) {
      touch();
      long sequence = clusterStateStore.nextSequence(CLUSTER_NAMESPACE, topic);
      latestSequence.accumulateAndGet(sequence, Math::max);
      clusterStateStore.appendEvent(CLUSTER_NAMESPACE, topic, sequence, json, replayBufferSize, replayTtl);
      for (Subscriber sub : subs.values()) {
        try {
          send(sub, json, sequence);
        } catch (Exception ex) {
          removeSubscriber(sub.id);
        }
      }
      clusterBus.publish(CLUSTER_NAMESPACE, new LiveClusterEvent(topic, sequence, json, Instant.now()));
      syncSubscribers();
    }

    private void send(Subscriber sub, String json, long seq) throws IOException {
      sub.emitter.send(SseEmitter.event()
          .name(EVENT_NAME)
          .id(Long.toString(seq))
          .data(json, MediaType.APPLICATION_JSON));
    }

    private String buildBatchJson(String topic, List<String> rawEvents) {
      StringBuilder sb = new StringBuilder(rawEvents.size() * 64);
      sb.append('{');
      sb.append("\"type\":\"BATCH\",");
      sb.append("\"topic\":\"").append(escape(topic)).append("\",");
      sb.append("\"at\":\"").append(Instant.now().toString()).append("\",");
      sb.append("\"events\":[");
      for (int i = 0; i < rawEvents.size(); i++) {
        if (i > 0) {
          sb.append(',');
        }
        sb.append(rawEvents.get(i));
      }
      sb.append("]}");
      return sb.toString();
    }

    private String escape(String s) {
      if (s == null) {
        return "";
      }
      return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
  }

  private static final class Subscriber {
    private final String id;
    private final SseEmitter emitter;

    Subscriber(String id, SseEmitter emitter) {
      this.id = id;
      this.emitter = emitter;
    }

    void closeQuiet() {
      try {
        emitter.complete();
      } catch (Exception ignored) {
      }
    }
  }
}
