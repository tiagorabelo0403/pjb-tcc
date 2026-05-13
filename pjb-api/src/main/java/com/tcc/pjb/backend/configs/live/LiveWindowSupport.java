package com.tcc.pjb.backend.configs.live;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class LiveWindowSupport {

    private LiveWindowSupport() {
    }

    public static <T> int forWindow(ConcurrentHashMap<String, T> values,
                                    AtomicInteger cursor,
                                    int limit,
                                    Consumer<T> consumer) {
        int size = values.size();
        if (size == 0) {
            return 0;
        }
        int safeLimit = Math.max(1, Math.min(limit, size));
        int start = Math.floorMod(cursor.getAndAdd(safeLimit), size);
        int processed = iterate(values, start, safeLimit, consumer);
        if (processed < safeLimit && start > 0) {
            processed += iterate(values, 0, safeLimit - processed, consumer);
        }
        return processed;
    }

    private static <T> int iterate(ConcurrentHashMap<String, T> values,
                                   int skip,
                                   int limit,
                                   Consumer<T> consumer) {
        int index = 0;
        int processed = 0;
        for (T value : values.values()) {
            if (value == null) {
                continue;
            }
            if (index++ < skip) {
                continue;
            }
            consumer.accept(value);
            processed++;
            if (processed >= limit) {
                break;
            }
        }
        return processed;
    }
}
