package com.tcc.pjb.backend.core.quality.codebase.application;

import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityAggregate;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

@Service
public class PjbCodebaseSanityApplicationService {

    private final PjbCodebaseSanityProjectLayout layout;
    private final PjbCodebaseSanitySettings settings;
    private final Clock clock;
    private final PjbCodebaseSanitySnapshotBuilder snapshotBuilder;
    private final AtomicReference<SnapshotCacheEntry> cache;

    public PjbCodebaseSanityApplicationService() {
        this(Path.of(""), PjbCodebaseSanitySettings.defaults(), Clock.systemUTC());
    }

    public PjbCodebaseSanityApplicationService(Path projectRoot) {
        this(projectRoot, PjbCodebaseSanitySettings.defaults(), Clock.systemUTC());
    }

    public PjbCodebaseSanityApplicationService(Path projectRoot,
                                               PjbCodebaseSanitySettings settings,
                                               Clock clock) {
        this.layout = PjbCodebaseSanityProjectLayout.fromProjectRoot(projectRoot);
        this.settings = Objects.requireNonNull(settings);
        this.clock = Objects.requireNonNull(clock);
        this.snapshotBuilder = new PjbCodebaseSanitySnapshotBuilder(layout, settings, new PjbCodebaseSanitySourceExplorer());
        this.cache = new AtomicReference<>();
    }

    public PjbCodebaseSanityAggregate auditar() {
        return auditar(false);
    }

    public PjbCodebaseSanityAggregate auditar(boolean forceRefresh) {
        Instant now = clock.instant();
        SnapshotCacheEntry current = cache.get();
        if (!forceRefresh && current != null && current.isFresh(now, settings)) {
            return current.aggregate();
        }
        PjbCodebaseSanityAggregate rebuilt = snapshotBuilder.build(now);
        SnapshotCacheEntry refreshed = new SnapshotCacheEntry(now, rebuilt);
        cache.set(refreshed);
        return rebuilt;
    }

    private record SnapshotCacheEntry(
            Instant createdAt,
            PjbCodebaseSanityAggregate aggregate
    ) {
        private SnapshotCacheEntry {
            createdAt = createdAt == null ? Instant.now() : createdAt;
            aggregate = Objects.requireNonNull(aggregate);
        }

        private boolean isFresh(Instant now, PjbCodebaseSanitySettings settings) {
            return !createdAt.plus(settings.cacheTtl()).isBefore(now);
        }
    }
}
