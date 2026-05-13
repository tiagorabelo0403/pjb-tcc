package com.tcc.pjb.backend.core.jobs.runtime;

import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleProfileResolver;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.springframework.stereotype.Component;

@Component
public class JobPermitLeaser {

    private static final int MAX_TRACKED_TYPES = 512;
    private static final int MAX_TRACKED_DIMENSIONS = 2048;
    private static final long IDLE_BUCKET_PRUNE_MILLIS = 300_000L;
    private static final long IDLE_STAT_PRUNE_MILLIS = 900_000L;

    public record Request(String type, String tenant, String uf, String orgao, String inboxKey) {
    }

    public static final class Lease implements AutoCloseable {

        private final String type;
        private final Semaphore global;
        private final PermitBucket perType;
        private final PermitBucket perTenant;
        private final PermitBucket perUf;
        private final PermitBucket perOrgao;
        private final PermitBucket perScaleProfile;

        private Lease(String type,
                      Semaphore global,
                      PermitBucket perType,
                      PermitBucket perTenant,
                      PermitBucket perUf,
                      PermitBucket perOrgao,
                      PermitBucket perScaleProfile) {
            this.type = type;
            this.global = global;
            this.perType = perType;
            this.perTenant = perTenant;
            this.perUf = perUf;
            this.perOrgao = perOrgao;
            this.perScaleProfile = perScaleProfile;
        }

        public String type() {
            return type;
        }

        @Override
        public void close() {
            try {
                if (perOrgao != null) {
                    perOrgao.release();
                }
            } finally {
                try {
                    if (perUf != null) {
                        perUf.release();
                    }
                } finally {
                    try {
                        if (perTenant != null) {
                            perTenant.release();
                        }
                    } finally {
                        try {
                            if (perScaleProfile != null) {
                                perScaleProfile.release();
                            }
                        } finally {
                            try {
                                if (perType != null) {
                                    perType.release();
                                }
                            } finally {
                                global.release();
                            }
                        }
                    }
                }
            }
        }
    }

    private final JobDispatcherProperties props;
    private final Semaphore global;
    private final ConcurrentHashMap<String, PermitBucket> perType = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PermitBucket> perTenant = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PermitBucket> perUf = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PermitBucket> perOrgao = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PermitBucket> perScaleProfile = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CounterBucket> done = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CounterBucket> failed = new ConcurrentHashMap<>();
    private final AtomicLong lastRebalanceEpochMs = new AtomicLong(0);
    private final JudicialScaleProfileResolver judicialScaleProfileResolver;

    public JobPermitLeaser(JobDispatcherProperties props, JudicialScaleProfileResolver judicialScaleProfileResolver) {
        this.props = Objects.requireNonNull(props);
        this.global = new Semaphore(Math.max(1, props.getMaxParallel()));
        this.judicialScaleProfileResolver = Objects.requireNonNull(judicialScaleProfileResolver);
    }

    public Lease acquire(String type) {
        return acquire(new Request(type, null, null, null, null));
    }

    public Lease acquire(Request req) {
        Objects.requireNonNull(req);
        String type = req.type() == null ? "" : req.type();
        maybeRebalance();

        global.acquireUninterruptibly();
        PermitBucket typeBucket = acquireBucket(perType, type, initialPerTypePermits(), MAX_TRACKED_TYPES);

        PermitBucket tenantBucket = null;
        PermitBucket ufBucket = null;
        PermitBucket orgaoBucket = null;
        PermitBucket profileBucket = null;
        JudicialScaleProfileResolver.JudicialScalePolicy scalePolicy = judicialScaleProfileResolver.resolvePolicyFromInbox(req.inboxKey(), req.type());

        try {
            tenantBucket = acquireQuota(perTenant, req.tenant(), props.getMaxParallelPerTenant());
            ufBucket = acquireQuota(perUf, req.uf(), props.getMaxParallelPerUf());
            orgaoBucket = acquireQuota(perOrgao, req.orgao(), props.getMaxParallelPerOrgao());
            profileBucket = acquireQuota(perScaleProfile, scaleProfileKey(scalePolicy), permitsForScaleProfile(scalePolicy));
            return new Lease(type, global, typeBucket, tenantBucket, ufBucket, orgaoBucket, profileBucket);
        } catch (Throwable t) {
            try {
                if (orgaoBucket != null) {
                    orgaoBucket.release();
                }
            } finally {
                try {
                    if (ufBucket != null) {
                        ufBucket.release();
                    }
                } finally {
                    try {
                        if (tenantBucket != null) {
                            tenantBucket.release();
                        }
                    } finally {
                        try {
                            if (profileBucket != null) {
                                profileBucket.release();
                            }
                        } finally {
                            try {
                                if (typeBucket != null) {
                                    typeBucket.release();
                                }
                            } finally {
                                global.release();
                            }
                        }
                    }
                }
            }
            throw t;
        }
    }

    public void recordSuccess(String type) {
        touchCounter(done, type == null ? "" : type, MAX_TRACKED_TYPES).increment();
    }

    public void recordFailure(String type) {
        touchCounter(failed, type == null ? "" : type, MAX_TRACKED_TYPES).increment();
    }

    private String scaleProfileKey(JudicialScaleProfileResolver.JudicialScalePolicy scalePolicy) {
        if (scalePolicy == null || scalePolicy.profile() == null) {
            return null;
        }
        return scalePolicy.profile().name();
    }

    private int permitsForScaleProfile(JudicialScaleProfileResolver.JudicialScalePolicy scalePolicy) {
        int globalMax = Math.max(1, props.getMaxParallel());
        double factor = scalePolicy == null ? 1d : scalePolicy.queueParallelismFactor();
        return Math.max(1, Math.min(globalMax, (int) Math.round(globalMax * factor)));
    }

    private PermitBucket acquireQuota(ConcurrentHashMap<String, PermitBucket> map, String key, int cap) {
        if (cap <= 0 || key == null || key.isBlank()) {
            return null;
        }
        return acquireBucket(map, key, cap, MAX_TRACKED_DIMENSIONS);
    }

    private PermitBucket acquireBucket(ConcurrentHashMap<String, PermitBucket> map, String key, int cap, int maxTracked) {
        long now = System.currentTimeMillis();
        pruneBuckets(map, now, maxTracked);
        PermitBucket bucket = map.computeIfAbsent(key, ignored -> new PermitBucket(cap));
        bucket.touch(now);
        bucket.acquire();
        return bucket;
    }

    private CounterBucket touchCounter(ConcurrentHashMap<String, CounterBucket> map, String key, int maxTracked) {
        long now = System.currentTimeMillis();
        pruneCounters(map, now, maxTracked);
        CounterBucket bucket = map.computeIfAbsent(key, ignored -> new CounterBucket());
        bucket.touch(now);
        return bucket;
    }

    private int initialPerTypePermits() {
        int configuredMax = Math.max(1, props.getMaxParallelPerType());
        int configuredMin = Math.max(1, props.getMinParallelPerType());
        int boundedMax = Math.min(configuredMax, Math.max(1, props.getMaxParallel()));
        return configuredMin > boundedMax ? configuredMin : boundedMax;
    }

    private void maybeRebalance() {
        long now = Instant.now().toEpochMilli();
        long last = lastRebalanceEpochMs.get();
        long every = Math.max(1000, props.getAdaptiveRebalanceMillis());
        if (now - last < every) {
            return;
        }
        if (!lastRebalanceEpochMs.compareAndSet(last, now)) {
            return;
        }

        pruneBuckets(perType, now, MAX_TRACKED_TYPES);
        pruneBuckets(perTenant, now, MAX_TRACKED_DIMENSIONS);
        pruneBuckets(perUf, now, MAX_TRACKED_DIMENSIONS);
        pruneBuckets(perOrgao, now, MAX_TRACKED_DIMENSIONS);
        pruneBuckets(perScaleProfile, now, MAX_TRACKED_DIMENSIONS);
        pruneCounters(done, now, MAX_TRACKED_TYPES);
        pruneCounters(failed, now, MAX_TRACKED_TYPES);

        int globalMax = Math.max(1, props.getMaxParallel());
        int min = Math.max(1, props.getMinParallelPerType());
        int max = Math.max(min, props.getMaxParallelPerType());

        long total = 0;
        for (var entry : done.entrySet()) {
            if (!perType.containsKey(entry.getKey())) {
                continue;
            }
            total += entry.getValue().sum();
        }
        total = Math.max(total, 1);

        for (var entry : perType.entrySet()) {
            String type = entry.getKey();
            PermitBucket bucket = entry.getValue();
            long d = done.getOrDefault(type, CounterBucket.ZERO).sum();
            long f = failed.getOrDefault(type, CounterBucket.ZERO).sum();

            double share = (double) d / (double) total;
            int target = (int) Math.round(share * globalMax);
            target = Math.max(min, Math.min(max, target));
            if (f > d && target > min) {
                target = Math.max(min, target - 1);
            }

            bucket.adjustCapacity(target);
            bucket.touch(now);
        }
    }

    private void pruneBuckets(ConcurrentHashMap<String, PermitBucket> map, long now, int maxTracked) {
        long idleCutoff = now - IDLE_BUCKET_PRUNE_MILLIS;
        map.entrySet().removeIf(entry -> entry.getValue().isIdle(idleCutoff));
        trimOverflowIdleBuckets(map, idleCutoff, maxTracked);
    }

    private void pruneCounters(ConcurrentHashMap<String, CounterBucket> map, long now, int maxTracked) {
        long idleCutoff = now - IDLE_STAT_PRUNE_MILLIS;
        map.entrySet().removeIf(entry -> entry.getValue().lastTouchedAt() <= idleCutoff && entry.getValue().sum() == 0L);
        trimOverflowCounters(map, maxTracked);
    }

    private void trimOverflowIdleBuckets(ConcurrentHashMap<String, PermitBucket> map, long idleCutoff, int maxTracked) {
        if (map.size() <= maxTracked) {
            return;
        }
        int toRemove = map.size() - maxTracked;
        map.entrySet().stream()
                .filter(entry -> entry.getValue().isIdle(idleCutoff))
                .sorted(Comparator.comparingLong(entry -> entry.getValue().lastTouchedAt()))
                .limit(Math.max(1, toRemove))
                .map(java.util.Map.Entry::getKey)
                .toList()
                .forEach(map::remove);
    }

    private void trimOverflowCounters(ConcurrentHashMap<String, CounterBucket> map, int maxTracked) {
        if (map.size() <= maxTracked) {
            return;
        }
        int toRemove = map.size() - maxTracked;
        map.entrySet().stream()
                .sorted(Comparator.comparingLong(entry -> entry.getValue().lastTouchedAt()))
                .limit(Math.max(1, toRemove))
                .map(java.util.Map.Entry::getKey)
                .toList()
                .forEach(map::remove);
    }

    private sealed interface TouchedBucket permits PermitBucket, CounterBucket {
        long lastTouchedAt();
    }

    private static final class PermitBucket implements TouchedBucket {

        private final ResizableSemaphore semaphore;
        private final AtomicLong lastTouchedAt = new AtomicLong(System.currentTimeMillis());

        private PermitBucket(int permits) {
            this.semaphore = new ResizableSemaphore(Math.max(1, permits));
        }

        void acquire() {
            touch(System.currentTimeMillis());
            semaphore.acquireUninterruptibly();
        }

        void release() {
            touch(System.currentTimeMillis());
            semaphore.release();
        }

        void adjustCapacity(int target) {
            int boundedTarget = Math.max(1, target);
            int inUse = semaphore.inUse();
            int desiredCapacity = Math.max(inUse, boundedTarget);
            int currentCapacity = semaphore.capacity();
            int delta = desiredCapacity - currentCapacity;
            if (delta > 0) {
                semaphore.expand(delta);
            } else if (delta < 0) {
                semaphore.reduce(-delta);
            }
        }

        void touch(long now) {
            lastTouchedAt.set(now);
        }

        boolean isIdle(long cutoff) {
            return lastTouchedAt.get() <= cutoff && semaphore.isIdle();
        }

        @Override
        public long lastTouchedAt() {
            return lastTouchedAt.get();
        }
    }

    private static final class CounterBucket implements TouchedBucket {

        private static final CounterBucket ZERO = new CounterBucket();

        private final LongAdder value = new LongAdder();
        private final AtomicLong lastTouchedAt = new AtomicLong(System.currentTimeMillis());

        void increment() {
            value.increment();
            touch(System.currentTimeMillis());
        }

        long sum() {
            return value.sum();
        }

        void touch(long now) {
            lastTouchedAt.set(now);
        }

        @Override
        public long lastTouchedAt() {
            return lastTouchedAt.get();
        }
    }
}
