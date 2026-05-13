package com.tcc.pjb.backend.configs.datasource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pjb.datasource.routing", name = "enabled", havingValue = "true")
public class PjbReplicaObservationService {

    private final PjbDataSourceRoutingProperties properties;
    private final DataSource writeDataSource;
    private final DataSource readDataSource;
    private final Map<String, DataSource> regionalReadDataSources;
    private final PjbReplicaFailoverTracker tracker;
    private volatile SnapshotCache cache;

    public PjbReplicaObservationService(PjbDataSourceRoutingProperties properties,
                                        @Qualifier("pjbWriteDataSource") DataSource writeDataSource,
                                        @Qualifier("pjbReadDataSource") DataSource readDataSource,
                                        @Qualifier("pjbRegionalReadReplicaDataSources") Map<String, DataSource> regionalReadDataSources,
                                        PjbReplicaFailoverTracker tracker) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.writeDataSource = Objects.requireNonNull(writeDataSource, "writeDataSource");
        this.readDataSource = Objects.requireNonNull(readDataSource, "readDataSource");
        this.regionalReadDataSources = Map.copyOf(Objects.requireNonNullElse(regionalReadDataSources, Map.of()));
        this.tracker = Objects.requireNonNull(tracker, "tracker");
    }

    public ReplicaObservationSnapshot currentSnapshot() {
        SnapshotCache current = cache;
        long now = System.currentTimeMillis();
        long ttl = sanitized(properties.getObservability().getRefreshTtl(), Duration.ofSeconds(5)).toMillis();
        if (current != null && current.expiresAtEpochMilli() > now) {
            return current.snapshot();
        }
        ReplicaObservationSnapshot snapshot = probeAll(now);
        cache = new SnapshotCache(snapshot, now + Math.max(250L, ttl));
        return snapshot;
    }

    private ReplicaObservationSnapshot probeAll(long now) {
        ReplicaNodeSnapshot write = probeNode("WRITE", writeDataSource, false);
        ReplicaNodeSnapshot read = probeNode("READ", readDataSource, true);
        LinkedHashMap<String, ReplicaNodeSnapshot> regional = new LinkedHashMap<>();
        regionalReadDataSources.forEach((key, dataSource) -> regional.put(key, probeNode(key, dataSource, true)));
        long availableRegional = regional.values().stream().filter(ReplicaNodeSnapshot::available).count();
        long configuredRegional = regional.size();
        return new ReplicaObservationSnapshot(
                Instant.ofEpochMilli(now),
                write,
                read,
                regional,
                tracker.isReplicaAvailable(),
                tracker.fallbackCount(),
                tracker.readSuccessCount(),
                tracker.readFailureCount(),
                configuredRegional,
                availableRegional
        );
    }

    private ReplicaNodeSnapshot probeNode(String lookupKey, DataSource dataSource, boolean replica) {
        Duration connectTimeout = sanitized(properties.getObservability().getConnectionTimeout(), Duration.ofSeconds(2));
        Duration statementTimeout = sanitized(properties.getObservability().getStatementTimeout(), Duration.ofSeconds(2));
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid((int) Math.max(1L, connectTimeout.toSeconds()))) {
                return new ReplicaNodeSnapshot(lookupKey, null, false, false, null, null, "connection-invalid");
            }
            DatabaseMetaData metaData = connection.getMetaData();
            String url = metaData == null ? null : metaData.getURL();
            String product = metaData == null ? null : metaData.getDatabaseProductName();
            boolean postgres = product != null && product.toLowerCase().contains("postgres");
            Boolean inRecovery = postgres ? queryBoolean(connection, "select pg_is_in_recovery()", statementTimeout) : null;
            Double replayLagSeconds = replica && postgres && Boolean.TRUE.equals(inRecovery)
                    ? queryDouble(connection, "select extract(epoch from coalesce(now() - pg_last_xact_replay_timestamp(), interval '0 seconds'))", statementTimeout)
                    : null;
            boolean available = !replica || Boolean.TRUE.equals(inRecovery);
            return new ReplicaNodeSnapshot(lookupKey, url, available, postgres, inRecovery, replayLagSeconds, null);
        } catch (SQLException ex) {
            return new ReplicaNodeSnapshot(lookupKey, null, false, false, null, null, ex.getMessage());
        }
    }

    private Boolean queryBoolean(Connection connection, String sql, Duration statementTimeout) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout((int) Math.max(1L, statementTimeout.toSeconds()));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBoolean(1);
                }
                return null;
            }
        }
    }

    private Double queryDouble(Connection connection, String sql, Duration statementTimeout) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout((int) Math.max(1L, statementTimeout.toSeconds()));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble(1);
                }
                return null;
            }
        }
    }

    private Duration sanitized(Duration candidate, Duration fallback) {
        if (candidate == null || candidate.isNegative() || candidate.isZero()) {
            return fallback;
        }
        return candidate;
    }

    private record SnapshotCache(ReplicaObservationSnapshot snapshot, long expiresAtEpochMilli) {
    }

    public record ReplicaObservationSnapshot(Instant observedAt,
                                             ReplicaNodeSnapshot write,
                                             ReplicaNodeSnapshot read,
                                             Map<String, ReplicaNodeSnapshot> regional,
                                             boolean failoverTrackerReplicaAvailable,
                                             long fallbackCount,
                                             long readSuccessCount,
                                             long readFailureCount,
                                             long configuredRegionalReplicas,
                                             long availableRegionalReplicas) {
    }

    public record ReplicaNodeSnapshot(String lookupKey,
                                      String jdbcUrl,
                                      boolean available,
                                      boolean postgres,
                                      Boolean inRecovery,
                                      Double replayLagSeconds,
                                      String error) {
    }
}
