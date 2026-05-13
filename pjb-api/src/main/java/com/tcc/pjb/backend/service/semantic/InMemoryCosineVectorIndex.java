package com.tcc.pjb.backend.service.semantic;

import java.util.Collections;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryCosineVectorIndex implements VectorIndex {

    private static final int MAX_ENTRIES = 20_000;

    private static final class Entry {
        final EmbeddingVector v;
        final Map<String, String> meta;
        volatile long lastTouchedNanos;

        Entry(EmbeddingVector v, Map<String, String> meta, long lastTouchedNanos) {
            this.v = v;
            this.meta = meta;
            this.lastTouchedNanos = lastTouchedNanos;
        }

        void touch(long now) {
            this.lastTouchedNanos = now;
        }
    }

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public void upsert(String id, EmbeddingVector vector, Map<String, String> metadata) {
        long now = System.nanoTime();
        store.put(Objects.requireNonNull(id), new Entry(vector.normalized(), metadata == null ? Map.of() : Collections.unmodifiableMap(metadata), now));
        trimIfNeeded();
    }

    @Override
    public List<VectorSearchHit> search(EmbeddingVector query, int topK, Map<String, String> filter) {
        int k = Math.max(1, topK);
        EmbeddingVector q = query.normalized();

        PriorityQueue<VectorSearchHit> heap = new PriorityQueue<>(Comparator.comparing(VectorSearchHit::score));
        long now = System.nanoTime();
        for (var e : store.entrySet()) {
            Entry entry = e.getValue();
            if (entry == null || !matches(entry.meta, filter)) continue;
            entry.touch(now);
            float score = EmbeddingVector.dot(q, entry.v);
            VectorSearchHit hit = new VectorSearchHit(e.getKey(), score, entry.meta);
            if (heap.size() < k) {
                heap.add(hit);
            } else if (score > heap.peek().score()) {
                heap.poll();
                heap.add(hit);
            }
        }

        ArrayList<VectorSearchHit> out = new ArrayList<>(heap);
        out.sort(Comparator.comparing(VectorSearchHit::score).reversed());
        return out;
    }

    @Override
    public int size() {
        return store.size();
    }

    private void trimIfNeeded() {
        int overflow = store.size() - MAX_ENTRIES;
        if (overflow <= 0) {
            return;
        }
        List<Map.Entry<String, Entry>> entries = new ArrayList<>(store.entrySet());
        entries.sort(Comparator.comparingLong(entry -> entry.getValue() == null ? Long.MIN_VALUE : entry.getValue().lastTouchedNanos));
        for (Map.Entry<String, Entry> entry : entries) {
            if (overflow <= 0) {
                break;
            }
            if (store.remove(entry.getKey(), entry.getValue())) {
                overflow--;
            }
        }
    }

    private static boolean matches(Map<String, String> meta, Map<String, String> filter) {
        if (filter == null || filter.isEmpty()) return true;
        for (var f : filter.entrySet()) {
            String v = meta.get(f.getKey());
            if (v == null) return false;
            if (!v.equalsIgnoreCase(f.getValue())) return false;
        }
        return true;
    }
}
