package com.tcc.pjb.backend.configs.datasource;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

public final class DataSourceIntrospectionSupport {

    private DataSourceIntrospectionSupport() {
    }

    public static HikariDataSource unwrapHikari(DataSource dataSource) {
        if (dataSource == null) {
            return null;
        }
        DataSource current = dataSource;
        for (int i = 0; i < 8 && current != null; i++) {
            if (current instanceof HikariDataSource hikariDataSource) {
                return hikariDataSource;
            }
            if (current instanceof LazyConnectionDataSourceProxy lazyProxy) {
                current = lazyProxy.getTargetDataSource();
                continue;
            }
            if (current instanceof DelegatingDataSource delegatingDataSource) {
                current = delegatingDataSource.getTargetDataSource();
                continue;
            }
            try {
                return current.unwrap(HikariDataSource.class);
            } catch (SQLException ignored) {
                return null;
            }
        }
        return null;
    }

    public static PoolSnapshot snapshot(DataSource dataSource) {
        HikariDataSource hikariDataSource = unwrapHikari(dataSource);
        if (hikariDataSource == null) {
            return PoolSnapshot.empty();
        }
        HikariPoolMXBean pool = hikariDataSource.getHikariPoolMXBean();
        if (pool == null) {
            return new PoolSnapshot(0, Math.max(1, hikariDataSource.getMaximumPoolSize()), 0, hikariDataSource.getPoolName());
        }
        return new PoolSnapshot(pool.getActiveConnections(), Math.max(1, hikariDataSource.getMaximumPoolSize()), pool.getThreadsAwaitingConnection(), hikariDataSource.getPoolName());
    }

    public record PoolSnapshot(int active, int maximum, int awaiting, String poolName) {
        public static PoolSnapshot empty() {
            return new PoolSnapshot(0, 1, 0, null);
        }

        public double activeRatio() {
            if (maximum <= 0) {
                return 0d;
            }
            return Math.max(0d, Math.min(1d, ((double) active) / ((double) maximum)));
        }
    }
}
