package com.tcc.pjb.backend.configs.datasource;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class PjbPrimaryReadPreferenceContext {

    private final ThreadLocal<AtomicLong> preferredUntil = ThreadLocal.withInitial(AtomicLong::new);

    public void preferPrimaryFor(Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            return;
        }
        preferPrimaryUntil(System.currentTimeMillis() + duration.toMillis());
    }

    public void preferPrimaryUntil(long epochMilliDeadline) {
        if (epochMilliDeadline <= System.currentTimeMillis()) {
            return;
        }
        preferredUntil.get().updateAndGet(current -> Math.max(current, epochMilliDeadline));
    }

    public boolean isPrimaryPreferred() {
        long deadline = preferredUntil.get().get();
        if (deadline <= 0L) {
            return false;
        }
        if (deadline > System.currentTimeMillis()) {
            return true;
        }
        clear();
        return false;
    }

    public long currentDeadlineEpochMilli() {
        if (!isPrimaryPreferred()) {
            return 0L;
        }
        return preferredUntil.get().get();
    }

    public void clear() {
        preferredUntil.remove();
    }
}
