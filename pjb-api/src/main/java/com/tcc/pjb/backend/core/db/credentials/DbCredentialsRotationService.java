package com.tcc.pjb.backend.core.db.credentials;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import javax.sql.DataSource;
import com.tcc.pjb.backend.platform.concurrent.PjbVirtualThreadSpine;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@ConditionalOnProperty(prefix = "pjb.db.credentials.rotation", name = "enabled", havingValue = "true")
public class DbCredentialsRotationService {

    private static final Logger log = LoggerFactory.getLogger(DbCredentialsRotationService.class);

    private final DbCredentialsRotationProperties props;
    private final DbCredentialsProvider provider;
    private final DataSource dataSource;
    private final AtomicBoolean rotationInFlight = new AtomicBoolean(false);

    public DbCredentialsRotationService(DbCredentialsRotationProperties props, DbCredentialsProvider provider, DataSource dataSource) {
        this.props = Objects.requireNonNull(props, "props");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Scheduled(fixedDelayString = "#{@dbCredentialsRotationProperties.refreshInterval.toMillis()}")
    public void rotate() {
        if (!props.isEnabled() || !rotationInFlight.compareAndSet(false, true)) return;
        PjbVirtualThreadSpine.start("db-credentials-rotation", () -> {
            try {
                rotateOnce();
            } finally {
                rotationInFlight.set(false);
            }
        });
    }

    void rotateOnce() {
        try (DbCredentials c = provider.fetch()) {
            HikariConfigMXBean config = unwrapConfigMxBean(dataSource);
            HikariPoolMXBean pool = unwrapPoolMxBean(dataSource);
            config.setUsername(c.username());
            String pw = new String(c.password());
            config.setPassword(pw);
            if (pool != null) {
                pool.softEvictConnections();
            }
            com.tcc.pjb.backend.core.security.crypto.StringWiper.tryWipe(pw);
            log.info("DB credentials rotated");
        } catch (Exception e) {
            log.error("DB credentials rotation failed", e);
        }
    }

    @SuppressWarnings("resource")
    private static HikariConfigMXBean unwrapConfigMxBean(DataSource ds) {
        HikariDataSource hk = unwrapManagedHikari(ds);
        return hk.getHikariConfigMXBean();
    }

    @SuppressWarnings("resource")
    private static HikariPoolMXBean unwrapPoolMxBean(DataSource ds) {
        HikariDataSource hk = unwrapManagedHikari(ds);
        return hk.getHikariPoolMXBean();
    }

    @SuppressWarnings("resource")
    private static HikariDataSource unwrapManagedHikari(DataSource ds) {
        if (ds instanceof HikariDataSource hk) return hk;
        try {
            return ds.unwrap(HikariDataSource.class);
        } catch (Exception e) {
            throw new IllegalStateException("DataSource is not Hikari", e);
        }
    }
}
