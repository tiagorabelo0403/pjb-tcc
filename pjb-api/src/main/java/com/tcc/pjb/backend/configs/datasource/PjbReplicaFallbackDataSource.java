package com.tcc.pjb.backend.configs.datasource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.AbstractDataSource;

public class PjbReplicaFallbackDataSource extends AbstractDataSource {

    private final DataSource readDataSource;
    private final DataSource writeDataSource;
    private final PjbReplicaFailoverTracker tracker;

    public PjbReplicaFallbackDataSource(DataSource readDataSource,
                                        DataSource writeDataSource,
                                        PjbReplicaFailoverTracker tracker) {
        this.readDataSource = Objects.requireNonNull(readDataSource, "readDataSource");
        this.writeDataSource = Objects.requireNonNull(writeDataSource, "writeDataSource");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (!tracker.isReplicaAvailable()) {
            return writeDataSource.getConnection();
        }
        try {
            Connection connection = readDataSource.getConnection();
            tracker.recordReplicaSuccess();
            return connection;
        } catch (SQLException ex) {
            if (!tracker.isFallbackToWriteOnError()) {
                throw ex;
            }
            tracker.recordReplicaFailure();
            return writeDataSource.getConnection();
        }
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        if (!tracker.isReplicaAvailable()) {
            return writeDataSource.getConnection(username, password);
        }
        try {
            Connection connection = readDataSource.getConnection(username, password);
            tracker.recordReplicaSuccess();
            return connection;
        } catch (SQLException ex) {
            if (!tracker.isFallbackToWriteOnError()) {
                throw ex;
            }
            tracker.recordReplicaFailure();
            return writeDataSource.getConnection(username, password);
        }
    }

    @Override
    public PrintWriter getLogWriter() {
        try {
            return readDataSource.getLogWriter();
        } catch (SQLException ex) {
            throw new IllegalStateException("Falha ao obter log writer do datasource de leitura", ex);
        }
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        try {
            readDataSource.setLogWriter(out);
            writeDataSource.setLogWriter(out);
        } catch (SQLException ex) {
            throw new IllegalStateException("Falha ao configurar log writer do datasource", ex);
        }
    }

    @Override
    public void setLoginTimeout(int seconds) {
        try {
            readDataSource.setLoginTimeout(seconds);
            writeDataSource.setLoginTimeout(seconds);
        } catch (SQLException ex) {
            throw new IllegalStateException("Falha ao configurar login timeout do datasource", ex);
        }
    }

    @Override
    public int getLoginTimeout() {
        try {
            return readDataSource.getLoginTimeout();
        } catch (SQLException ex) {
            throw new IllegalStateException("Falha ao obter login timeout do datasource", ex);
        }
    }

    @Override
    public Logger getParentLogger() {
        try {
            return readDataSource.getParentLogger();
        } catch (SQLFeatureNotSupportedException ex) {
            return Logger.getLogger(PjbReplicaFallbackDataSource.class.getName());
        }
    }
}
