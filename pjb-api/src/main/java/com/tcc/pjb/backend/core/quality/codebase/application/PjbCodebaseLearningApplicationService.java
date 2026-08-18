package com.tcc.pjb.backend.core.quality.codebase.application;

import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseLearningAggregate;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;
import jakarta.inject.Inject;

@Service
public class PjbCodebaseLearningApplicationService {

    private final PjbCodebaseProjectLayout layout;
    private final PjbCodebaseLearningSettings settings;
    private final Clock clock;
    private final PjbCodebaseLearningSnapshotBuilder snapshotBuilder;
    private final AtomicReference<SnapshotCacheEntry> cache;

    @Inject
    public PjbCodebaseLearningApplicationService() {
        this(Path.of(""), PjbCodebaseLearningSettings.defaults(), Clock.systemUTC());
    }

    public PjbCodebaseLearningApplicationService(Path projectRoot) {
        this(projectRoot, PjbCodebaseLearningSettings.defaults(), Clock.systemUTC());
    }

    public PjbCodebaseLearningApplicationService(Path projectRoot,
                                          PjbCodebaseLearningSettings settings,
                                          Clock clock) {
        this.layout = PjbCodebaseProjectLayout.fromProjectRoot(projectRoot);
        this.settings = Objects.requireNonNull(settings);
        this.clock = Objects.requireNonNull(clock);
        this.snapshotBuilder = new PjbCodebaseLearningSnapshotBuilder(layout, settings, new PjbCodebaseSourceExplorer());
        this.cache = new AtomicReference<>();
    }

    public PjbCodebaseLearningAggregate aprender() {
        return aprender(false);
    }

    public PjbCodebaseLearningAggregate aprender(boolean forceRefresh) {
        Instant now = clock.instant();
        SnapshotCacheEntry current = cache.get();
        if (!forceRefresh && current != null && current.isFresh(now, settings)) {
            return current.aggregate();
        }
        PjbCodebaseLearningAggregate rebuilt = snapshotBuilder.build(now);
        SnapshotCacheEntry refreshed = new SnapshotCacheEntry(now, rebuilt);
        cache.set(refreshed);
        return rebuilt;
    }

    private record SnapshotCacheEntry(
            Instant createdAt,
            PjbCodebaseLearningAggregate aggregate
    ) {
        private SnapshotCacheEntry {
            createdAt = createdAt == null ? Instant.now() : createdAt;
            aggregate = Objects.requireNonNull(aggregate);
        }

        private boolean isFresh(Instant now, PjbCodebaseLearningSettings settings) {
            return !createdAt.plus(settings.cacheTtl()).isBefore(now);
        }
    }
}
