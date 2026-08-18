package com.tcc.pjb.backend.core.id;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class PjbUuidV7Generator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final AtomicLong STATE = new AtomicLong();

    private PjbUuidV7Generator() {
    }

    public static UUID generate() {
        long state = nextState();
        long currentMs = state >>> 12;
        long seq = state & 0x0FFFL;
        long random = RANDOM.nextLong();
        long msb = (currentMs << 16) | 0x7000L | seq;
        long lsb = 0x8000000000000000L | (random & 0x3FFFFFFFFFFFFFFFL);
        return new UUID(msb, lsb);
    }

    private static long nextState() {
        while (true) {
            long previous = STATE.get();
            long previousMs = previous >>> 12;
            long previousSeq = previous & 0x0FFFL;
            long currentMs = Math.max(System.currentTimeMillis(), previousMs);
            long seq = currentMs == previousMs ? (previousSeq + 1) & 0x0FFFL : 0L;
            if (currentMs == previousMs && seq == 0L) {
                currentMs = previousMs + 1;
            }
            long next = (currentMs << 12) | seq;
            if (STATE.compareAndSet(previous, next)) {
                return next;
            }
        }
    }
}
