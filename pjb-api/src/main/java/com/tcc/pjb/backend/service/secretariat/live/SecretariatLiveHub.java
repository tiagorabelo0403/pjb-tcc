package com.tcc.pjb.backend.service.secretariat.live;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SecretariatLiveHub {

  private static final Logger log = LoggerFactory.getLogger(SecretariatLiveHub.class);

  private static final String EVENT_NAME = "secretariat";
  private static final String CLUSTER_NAMESPACE = "secretariat";

  private final ExecutorService io;
  private final LiveClusterBus clusterBus;
  private final LiveClusterStateStore clusterStateStore;
  private final int replayBufferSize;
  private final int maxBatchEvents;
  private final int maxPendingCoalescedKeys;
  private final long emitterTimeoutMs;
  private final int maxChannels;
  private final int maxSubscribersPerTopic;
  private final int maxChannelsPerFlushCycle;
  private final int maxChannelsPerRefreshCycle;
  private final long idleChannelTtlNanos;
  private final Duration replayTtl;
  private final Duration subscriberTtl;
  private final ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>();
  private final AtomicInteger flushCursor = new AtomicInteger(0);
  private final AtomicInteger refreshCursor = new AtomicInteger(0);
  private final AtomicInteger heartbeatCursor = new AtomicInteger(0);

  public SecretariatLiveHub(
      @Qualifier("pjbLiveExecutorService") ExecutorService io,
      LiveClusterBus clusterBus,
      LiveClusterStateStore clusterStateStore,
      @Value("${pjb.secretariat.sse.replayBuffer:300}") int replayBufferSize,
      @Value("${pjb.secretariat.sse.maxBatchEvents:200}") int maxBatchEvents,
      @Value("${pjb.secretariat.sse.maxPendingCoalescedKeys:2000}") int maxPendingCoalescedKeys,
      @Value("${pjb.secretariat.sse.emitterTimeoutMs:1800000}") long emitterTimeoutMs,
      @Value("${pjb.secretariat.sse.maxChannels:4096}") int maxChannels,
      @Value("${pjb.secretariat.sse.maxSubscribersPerTopic:8}") int maxSubscribersPerTopic,
      @Value("${pjb.secretariat.sse.maxChannelsPerFlushCycle:256}") int maxChannelsPerFlushCycle,
      @Value("${pjb.secretariat.sse.maxChannelsPerRefreshCycle:512}") int maxChannelsPerRefreshCycle,
      @Value("${pjb.secretariat.sse.idleChannelTtl:5m}") Duration idleChannelTtl,
      @Value("${pjb.live.cluster.replay-ttl-seconds:900}") long replayTtlSeconds,
      @Value("${pjb.live.cluster.subscriber-ttl-seconds:120}") long subscriberTtlSeconds
  ) {
    this.replayBufferSize = Math.max(50, replayBufferSize);
    this.maxBatchEvents = Math.max(50, maxBatchEvents);
    this.maxPendingCoalescedKeys = Math.max(this.maxBatchEvents, maxPendingCoalescedKeys);
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

  public SseEmitter register(String inboxKey, String lastEventId) {
    Objects.requireNonNull(inboxKey, "inboxKey");
    Channel ch = registrationChannel(inboxKey);
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

  public void enqueueRaw(String inboxKey, String json) {
    if (inboxKey == null || inboxKey.isBlank() || json == null || json.isBlank()) {
      return;
    }
    Channel ch = publishChannel(inboxKey);
    if (ch == null) {
      return;
    }
    ch.enqueue(deriveCoalesceKey(json), json);
  }

  @Scheduled(fixedDelayString = "${pjb.secretariat.sse.flushMs:200}")
  public void flushPending() {
    LiveWindowSupport.forWindow(channels, flushCursor, maxChannelsPerFlushCycle, ch -> {
      try {
        ch.flushBatches(maxBatchEvents);
      } catch (Exception ex) {
        log.debug("secretariat flush failed: {}", ex.getMessage());
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
    LiveWindowSupport.forWindow(channels, heartbeatCursor, maxChannelsPerRefreshCycle, ch -> ch.enqueue("__heartbeat__", hb));
  }

  private Channel registrationChannel(String inboxKey) {
    Channel existing = channels.get(inboxKey);
    if (existing != null) {
      existing.touch();
      return existing;
    }
    cleanupIdleChannels();
    Channel created = new Channel(inboxKey, maxPendingCoalescedKeys);
    Channel prior = channels.putIfAbsent(inboxKey, created);
    Channel result = prior == null ? created : prior;
    if (prior == null && channels.size() > maxChannels) {
      channels.remove(inboxKey, created);
      throw new TooManySseConnectionsException("too many sse");
    }
    result.touch();
    return result;
  }

  private Channel publishChannel(String inboxKey) {
    Channel existing = channels.get(inboxKey);
    if (existing != null) {
      existing.touch();
      return existing;
    }
    cleanupIdleChannels();
    Channel created = new Channel(inboxKey, maxPendingCoalescedKeys);
    Channel prior = channels.putIfAbsent(inboxKey, created);
    Channel result = prior == null ? created : prior;
    if (prior == null && channels.size() > maxChannels) {
      channels.remove(inboxKey, created);
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

  private static String deriveCoalesceKey(String json) {
    Long wid = extractLong(json, "\"workItemId\":");
    if (wid != null) {
      String type = extractString(json, "\"type\":\"");
      return (type == null ? "evt" : type) + ":wid:" + wid;
    }
    Long pid = extractLong(json, "\"processoId\":");
    if (pid != null) {
      String type = extractString(json, "\"type\":\"");
      return (type == null ? "evt" : type) + ":pid:" + pid;
    }
    return "u:" + UUID.randomUUID();
  }

  private static Long extractLong(String json, String needle) {
    int p = json.indexOf(needle);
    if (p < 0) {
      return null;
    }
    int i = p + needle.length();
    while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
      i++;
    }
    int start = i;
    while (i < json.length()) {
      char c = json.charAt(i);
      if (c < '0' || c > '9') {
        break;
      }
      i++;
    }
    if (i == start) {
      return null;
    }
    try {
      return Long.parseLong(json.substring(start, i));
    } catch (Exception ignored) {
      return null;
    }
  }

  private static String extractString(String json, String needle) {
    int p = json.indexOf(needle);
    if (p < 0) {
      return null;
    }
    int i = p + needle.length();
    int end = json.indexOf('"', i);
    if (end < 0) {
      return null;
    }
    return json.substring(i, end);
  }

  private final class Channel {
    private final String inboxKey;
    private final int maxPendingCoalescedKeys;
    private final AtomicLong latestSequence = new AtomicLong(0);
    private final AtomicLong lastTouchedNanos = new AtomicLong(System.nanoTime());
    private final ConcurrentHashMap<String, Subscriber> subs = new ConcurrentHashMap<>();
    private final AtomicInteger pendingKeyCount = new AtomicInteger(0);
    private final AtomicReference<ConcurrentHashMap<String, String>> pending = new AtomicReference<>(new ConcurrentHashMap<>());

    Channel(String inboxKey, int maxPendingCoalescedKeys) {
      this.inboxKey = inboxKey;
      this.maxPendingCoalescedKeys = maxPendingCoalescedKeys;
    }

    String key() {
      return inboxKey;
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
        Subscriber sub = new Subscriber(id, emitter);
        if (subs.putIfAbsent(id, sub) == null) {
          touch();
          return sub;
        }
      }
    }

    void removeSubscriber(String id) {
      Subscriber sub = subs.remove(id);
      if (sub != null) {
        touch();
        sub.closeQuiet();
      }
    }

    void syncSubscribers() {
      clusterStateStore.syncSubscriberCount(CLUSTER_NAMESPACE, inboxKey, subs.size(), subscriberTtl);
    }

    boolean isRemovable(long nowNanos) {
      return subs.isEmpty()
          && pendingKeyCount.get() == 0
          && nowNanos - lastTouchedNanos.get() >= idleChannelTtlNanos;
    }

    void enqueue(String coalesceKey, String json) {
      touch();
      ConcurrentHashMap<String, String> current = pending.get();
      String previous = current.putIfAbsent(coalesceKey, json);
      if (previous != null) {
        current.put(coalesceKey, json);
        return;
      }
      int size = pendingKeyCount.incrementAndGet();
      if (size > maxPendingCoalescedKeys) {
        current.remove(coalesceKey, json);
        pendingKeyCount.decrementAndGet();
      }
    }

    void flushBatches(int maxBatchEvents) {
      touch();
      if (subs.isEmpty() && !clusterBus.enabled() && !clusterStateStore.distributed()) {
        pending.getAndSet(new ConcurrentHashMap<>());
        pendingKeyCount.set(0);
        return;
      }
      ConcurrentHashMap<String, String> snapshot = pending.getAndSet(new ConcurrentHashMap<>());
      pendingKeyCount.set(0);
      if (snapshot.isEmpty()) {
        return;
      }
      List<String> events = new ArrayList<>(snapshot.values());
      for (int start = 0; start < events.size(); start += maxBatchEvents) {
        int end = Math.min(events.size(), start + maxBatchEvents);
        publishLocal(buildBatchJson(inboxKey, events.subList(start, end)));
      }
    }

    void replayTo(Subscriber sub, long wantSeq) throws IOException {
      touch();
      long latest = Math.max(latestSequence.get(), clusterStateStore.latestSequence(CLUSTER_NAMESPACE, inboxKey));
      if (wantSeq >= 0 && latest - wantSeq > replayBufferSize) {
        send(sub, "{\"type\":\"RESYNC_REQUIRED\",\"at\":\"" + Instant.now().toString() + "\"}", latest);
        return;
      }
      List<LiveClusterStateStore.ReplayEntry> entries = clusterStateStore.replayAfter(CLUSTER_NAMESPACE, inboxKey, wantSeq, replayBufferSize);
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
      long sequence = clusterStateStore.nextSequence(CLUSTER_NAMESPACE, inboxKey);
      latestSequence.accumulateAndGet(sequence, Math::max);
      clusterStateStore.appendEvent(CLUSTER_NAMESPACE, inboxKey, sequence, json, replayBufferSize, replayTtl);
      for (Subscriber sub : subs.values()) {
        try {
          send(sub, json, sequence);
        } catch (Exception ex) {
          removeSubscriber(sub.id);
        }
      }
      clusterBus.publish(CLUSTER_NAMESPACE, new LiveClusterEvent(inboxKey, sequence, json, Instant.now()));
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
