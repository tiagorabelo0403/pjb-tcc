package com.tcc.pjb.backend.configs.datasource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.AbstractDataSource;

public class PjbWriteFailoverDataSource extends AbstractDataSource {

    private final String primaryName;
    private final DataSource primaryDataSource;
    private final Map<String, DataSource> candidates;
    private final PjbWriteFailoverTracker tracker;
    private final boolean strict;

    public PjbWriteFailoverDataSource(String primaryName,
                                      DataSource primaryDataSource,
                                      Map<String, DataSource> candidates,
                                      PjbWriteFailoverTracker tracker,
                                      boolean strict) {
        this.primaryName = normalizeName(primaryName);
        this.primaryDataSource = Objects.requireNonNull(primaryDataSource, "primaryDataSource");
        this.candidates = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNullElse(candidates, Map.of())));
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.strict = strict;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return acquire(() -> null);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return acquire(() -> new Credentials(username, password));
    }

    private Connection acquire(Supplier<Credentials> credentialsSupplier) throws SQLException {
        List<String> orderedNames = tracker.prioritize(primaryName, new ArrayList<>(candidates.keySet()));
        SQLException first = null;
        String previousSuccess = tracker.preferredEndpoint();
        for (String name : orderedNames) {
            if (!tracker.isEndpointAvailable(name)) {
                continue;
            }
            DataSource dataSource = resolve(name);
            if (dataSource == null) {
                continue;
            }
            try {
                Credentials credentials = credentialsSupplier.get();
                Connection connection = credentials == null
                        ? dataSource.getConnection()
                        : dataSource.getConnection(credentials.username(), credentials.password());
                tracker.recordSuccess(name);
                if (previousSuccess != null && !previousSuccess.equals(name)) {
                    tracker.recordFailover(previousSuccess, name);
                } else if (!primaryName.equals(name)) {
                    tracker.recordFailover(primaryName, name);
                }
                return connection;
            } catch (SQLException ex) {
                tracker.recordFailure(name);
                if (first == null) {
                    first = ex;
                }
                if (strict && primaryName.equals(name)) {
                    throw ex;
                }
            }
        }
        if (first != null) {
            throw first;
        }
        throw new SQLException("Nenhum datasource de escrita disponível para o PJB");
    }

    private DataSource resolve(String name) {
        if (primaryName.equals(name)) {
            return primaryDataSource;
        }
        return candidates.get(name);
    }

    @Override
    public PrintWriter getLogWriter() {
        try {
            return primaryDataSource.getLogWriter();
        } catch (SQLException ex) {
            throw new IllegalStateException("Falha ao obter log writer do datasource de escrita", ex);
        }
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        try {
            primaryDataSource.setLogWriter(out);
            for (DataSource candidate : candidates.values()) {
                candidate.setLogWriter(out);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Falha ao configurar log writer do datasource de escrita", ex);
        }
    }

    @Override
    public void setLoginTimeout(int seconds) {
        try {
            primaryDataSource.setLoginTimeout(seconds);
            for (DataSource candidate : candidates.values()) {
                candidate.setLoginTimeout(seconds);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Falha ao configurar login timeout do datasource de escrita", ex);
        }
    }

    @Override
    public int getLoginTimeout() {
        try {
            return primaryDataSource.getLoginTimeout();
        } catch (SQLException ex) {
            throw new IllegalStateException("Falha ao obter login timeout do datasource de escrita", ex);
        }
    }

    @Override
    public Logger getParentLogger() {
        try {
            return primaryDataSource.getParentLogger();
        } catch (SQLFeatureNotSupportedException ex) {
            return Logger.getLogger(PjbWriteFailoverDataSource.class.getName());
        }
    }

    private static String normalizeName(String value) {
        String normalized = value == null ? null : value.trim().toUpperCase();
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("Nome do datasource de escrita obrigatório");
        }
        return normalized;
    }

    private record Credentials(String username, String password) {
    }
}
