package com.tcc.pjb.backend.modules.atendimento.service;

import com.tcc.pjb.backend.configs.live.LiveWindowSupport;
import com.tcc.pjb.backend.service.sse.TooManySseConnectionsException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
public class AtendimentoInboxLiveHub {

  private static final Logger log = LoggerFactory.getLogger(AtendimentoInboxLiveHub.class);

  private static final String EVENT_NAME = "atendimento";

  private final int replayBufferSize;
  private final int maxBatchEvents;
  private final int maxPendingBacklog;
  private final long emitterTimeoutMs;
  private final int maxChannels;
  private final int maxSubscribersPerTopic;
  private final int maxChannelsPerFlushCycle;
  private final long idleChannelTtlNanos;

  private final ExecutorService io;
  private final ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>();
  private final AtomicInteger flushCursor = new AtomicInteger(0);
  private final AtomicInteger heartbeatCursor = new AtomicInteger(0);

  public AtendimentoInboxLiveHub(
      @Qualifier("pjbLiveExecutorService") ExecutorService io,
      @Value("${pjb.atendimento.sse.replayBuffer:200}") int replayBufferSize,
      @Value("${pjb.atendimento.sse.maxBatchEvents:200}") int maxBatchEvents,
      @Value("${pjb.atendimento.sse.maxPendingBacklog:2000}") int maxPendingBacklog,
      @Value("${pjb.atendimento.sse.emitterTimeoutMs:1800000}") long emitterTimeoutMs,
      @Value("${pjb.atendimento.sse.maxChannels:2048}") int maxChannels,
      @Value("${pjb.atendimento.sse.maxSubscribersPerTopic:5}") int maxSubscribersPerTopic,
      @Value("${pjb.atendimento.sse.maxChannelsPerFlushCycle:256}") int maxChannelsPerFlushCycle,
      @Value("${pjb.atendimento.sse.idleChannelTtl:5m}") Duration idleChannelTtl
  ) {
    this.replayBufferSize = Math.max(50, replayBufferSize);
    this.maxBatchEvents = Math.max(50, maxBatchEvents);
    this.maxPendingBacklog = Math.max(this.maxBatchEvents, maxPendingBacklog);
    this.emitterTimeoutMs = Math.max(60_000L, emitterTimeoutMs);
    this.maxChannels = Math.max(64, maxChannels);
    this.maxSubscribersPerTopic = Math.max(1, maxSubscribersPerTopic);
    this.maxChannelsPerFlushCycle = Math.max(1, maxChannelsPerFlushCycle);
    this.idleChannelTtlNanos = Objects.requireNonNull(idleChannelTtl, "idleChannelTtl").toNanos();
    this.io = Objects.requireNonNull(io, "io");
  }

  public SseEmitter register(String topic, String lastEventId) {
    Objects.requireNonNull(topic, "topic");
    Channel ch = registrationChannel(topic);
    SseEmitter emitter = new SseEmitter(emitterTimeoutMs);
    long wantSeq = parseLastEventId(lastEventId).orElse(-1L);
    Subscriber sub = ch.addSubscriber(emitter);
    emitter.onCompletion(() -> {
      ch.removeSubscriber(sub.id);
      removeIfIdle(ch);
    });
    emitter.onTimeout(() -> {
      ch.removeSubscriber(sub.id);
      removeIfIdle(ch);
    });
    emitter.onError(ex -> {
      ch.removeSubscriber(sub.id);
      removeIfIdle(ch);
    });
    io.execute(() -> {
      try {
        ch.replayTo(sub, wantSeq);
      } catch (Exception ex) {
        ch.removeSubscriber(sub.id);
        removeIfIdle(ch);
      }
    });
    return emitter;
  }

  public int activeSubscribers(String topic) {
    if (topic == null || topic.isBlank()) {
      return 0;
    }
    Channel ch = channels.get(topic);
    return ch == null ? 0 : ch.subscriberCount();
  }

  public void enqueue(String topic, String json) {
    if (topic == null || topic.isBlank() || json == null || json.isBlank()) {
      return;
    }
    Channel ch = channels.get(topic);
    if (ch == null) {
      return;
    }
    ch.enqueue(json);
  }

  @Scheduled(fixedDelayString = "${pjb.atendimento.sse.flushMs:200}")
  public void flushPending() {
    LiveWindowSupport.forWindow(channels, flushCursor, maxChannelsPerFlushCycle, ch -> {
      try {
        ch.flushBatches(maxBatchEvents);
      } catch (Exception ex) {
        log.debug("atendimento flush failed: {}", ex.getMessage());
      }
      removeIfIdle(ch);
    });
  }

  public void heartbeat() {
    String hb = "{\"type\":\"HEARTBEAT\",\"at\":\"" + Instant.now().toString() + "\"}";
    LiveWindowSupport.forWindow(channels, heartbeatCursor, maxChannelsPerFlushCycle, ch -> ch.enqueue(hb));
  }

  private Channel registrationChannel(String topic) {
    Channel existing = channels.get(topic);
    if (existing != null) {
      existing.touch();
      return existing;
    }
    cleanupIdleChannels();
    Channel created = new Channel(topic, replayBufferSize, maxPendingBacklog);
    Channel prior = channels.putIfAbsent(topic, created);
    Channel result = prior == null ? created : prior;
    if (prior == null && channels.size() > maxChannels) {
      channels.remove(topic, created);
      throw new TooManySseConnectionsException("too many sse");
    }
    result.touch();
    return result;
  }

  private void cleanupIdleChannels() {
    long now = System.nanoTime();
    for (Channel ch : channels.values()) {
      if (ch.isRemovable(now)) {
        channels.remove(ch.key(), ch);
      }
    }
  }

  private void removeIfIdle(Channel ch) {
    long now = System.nanoTime();
    if (ch.isRemovable(now)) {
      channels.remove(ch.key(), ch);
    }
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
    private final int replaySize;
    private final int maxPendingBacklog;
    private final AtomicLong seq = new AtomicLong(0);
    private final AtomicInteger pendingCount = new AtomicInteger(0);
    private final AtomicLong lastTouchedNanos = new AtomicLong(System.nanoTime());
    private final ConcurrentHashMap<String, Subscriber> subs = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> pending = new ConcurrentLinkedQueue<>();
    private final Object lock = new Object();
    private final Deque<Entry> buffer;

    Channel(String topic, int replaySize, int maxPendingBacklog) {
      this.topic = topic;
      this.replaySize = replaySize;
      this.maxPendingBacklog = maxPendingBacklog;
      this.buffer = new ArrayDeque<>(replaySize);
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
      if (subs.isEmpty()) {
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
      publishNow(buildBatchJson(topic, slice));
    }

    void replayTo(Subscriber sub, long wantSeq) throws IOException {
      touch();
      long current = seq.get();
      if (wantSeq >= 0 && current - wantSeq > replaySize) {
        send(sub, "{\"type\":\"RESYNC_REQUIRED\",\"at\":\"" + Instant.now().toString() + "\"}", current);
        return;
      }
      List<Entry> entries;
      synchronized (lock) {
        entries = new ArrayList<>(buffer);
      }
      for (Entry e : entries) {
        if (wantSeq < 0 || e.seq > wantSeq) {
          send(sub, e.json, e.seq);
        }
      }
    }

    private void publishNow(String json) {
      touch();
      long s = seq.incrementAndGet();
      Entry entry = new Entry(s, json);
      synchronized (lock) {
        buffer.addLast(entry);
        while (buffer.size() > replaySize) {
          buffer.removeFirst();
        }
      }
      for (Subscriber sub : subs.values()) {
        try {
          send(sub, json, s);
        } catch (Exception ex) {
          removeSubscriber(sub.id);
        }
      }
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

  private record Entry(long seq, String json) {
  }
}
