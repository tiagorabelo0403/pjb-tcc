package com.tcc.pjb.backend.core.observability.systemhealth;

import com.tcc.pjb.backend.configs.datasource.DataSourceIntrospectionSupport;
import com.tcc.pjb.backend.configs.datasource.DataSourceIntrospectionSupport.PoolSnapshot;
import com.tcc.pjb.backend.configs.security.hardening.ApiDatabasePressureShieldProperties;
import com.tcc.pjb.backend.configs.security.hardening.PjbFunctionalAvailabilityProperties;
import com.tcc.pjb.backend.service.outbox.observability.PjbOutboxMetricsService;
import com.tcc.pjb.backend.service.outbox.observability.PjbOutboxObservabilityProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class PjbFunctionalAvailabilityService {

    private final PjbFunctionalAvailabilityProperties properties;
    private final ApiDatabasePressureShieldProperties pressureShieldProperties;
    private final ObjectProvider<DataSource> writeDataSourceProvider;
    private final ObjectProvider<DataSource> readDataSourceProvider;
    private final ObjectProvider<DataSource> applicationDataSourceProvider;
    private final ObjectProvider<PjbOutboxMetricsService> outboxMetricsServiceProvider;
    private final ObjectProvider<PjbOutboxObservabilityProperties> outboxObservabilityPropertiesProvider;
    private final ObjectProvider<PjbOperationalCrisisService> crisisServiceProvider;

    public PjbFunctionalAvailabilityService(PjbFunctionalAvailabilityProperties properties,
                                            ApiDatabasePressureShieldProperties pressureShieldProperties,
                                            @Qualifier("pjbWriteDataSource") ObjectProvider<DataSource> writeDataSourceProvider,
                                            @Qualifier("pjbReadDataSource") ObjectProvider<DataSource> readDataSourceProvider,
                                            ObjectProvider<DataSource> applicationDataSourceProvider,
                                            ObjectProvider<PjbOutboxMetricsService> outboxMetricsServiceProvider,
                                            ObjectProvider<PjbOutboxObservabilityProperties> outboxObservabilityPropertiesProvider,
                                            ObjectProvider<PjbOperationalCrisisService> crisisServiceProvider) {
        this.properties = properties;
        this.pressureShieldProperties = pressureShieldProperties;
        this.writeDataSourceProvider = writeDataSourceProvider;
        this.readDataSourceProvider = readDataSourceProvider;
        this.applicationDataSourceProvider = applicationDataSourceProvider;
        this.outboxMetricsServiceProvider = outboxMetricsServiceProvider;
        this.outboxObservabilityPropertiesProvider = outboxObservabilityPropertiesProvider;
        this.crisisServiceProvider = crisisServiceProvider;
    }

    public FunctionalReadiness readiness(PjbFunctionalDomain domain) {
        if (!properties.isEnabled()) {
            return new FunctionalReadiness(domain, true, "availability-disabled", snapshot());
        }
        if (!properties.isAvailable(domain)) {
            return new FunctionalReadiness(domain, false, "dominio desabilitado por governanca operacional", snapshot());
        }
        SystemSnapshot snapshot = snapshot();
        if (domain == PjbFunctionalDomain.PETICIONAMENTO || domain == PjbFunctionalDomain.OPERACAO_INTERNA) {
            if (snapshot.writePool().activeRatio() >= pressureShieldProperties.getWriteActiveRatioThreshold()) {
                return new FunctionalReadiness(domain, false, "pool de escrita em saturacao para operacao critica", snapshot);
            }
            if (snapshot.writePool().awaiting() >= pressureShieldProperties.getWriteThreadsAwaitingThreshold()) {
                return new FunctionalReadiness(domain, false, "threads aguardando conexao de escrita acima do limite operacional", snapshot);
            }
        }
        if (domain == PjbFunctionalDomain.CONSULTA || domain == PjbFunctionalDomain.INTEGRACOES || domain == PjbFunctionalDomain.COMUNICACAO) {
            if (snapshot.readPool().activeRatio() >= pressureShieldProperties.getReadActiveRatioThreshold()) {
                return new FunctionalReadiness(domain, false, "pool de leitura em saturacao para carga consultiva", snapshot);
            }
            if (snapshot.readPool().awaiting() >= pressureShieldProperties.getReadThreadsAwaitingThreshold()) {
                return new FunctionalReadiness(domain, false, "threads aguardando conexao de leitura acima do limite operacional", snapshot);
            }
        }
        if (snapshot.outboxPendingLagExceeded() && domain == PjbFunctionalDomain.INTEGRACOES) {
            return new FunctionalReadiness(domain, false, "outbox acumulado acima do lag operacional tolerado", snapshot);
        }
        return new FunctionalReadiness(domain, true, "ready", snapshot);
    }

    public SystemSnapshot snapshot() {
        DataSource writeSource = choose(writeDataSourceProvider.getIfAvailable(), applicationDataSourceProvider.getIfAvailable());
        DataSource readSource = choose(readDataSourceProvider.getIfAvailable(), writeSource);
        PoolSnapshot writePool = DataSourceIntrospectionSupport.snapshot(writeSource);
        PoolSnapshot readPool = DataSourceIntrospectionSupport.snapshot(readSource);
        PjbOutboxMetricsService outboxMetricsService = outboxMetricsServiceProvider.getIfAvailable();
        PjbOutboxObservabilityProperties outboxProperties = outboxObservabilityPropertiesProvider.getIfAvailable();
        long outboxPending = outboxMetricsService != null ? outboxMetricsService.pending() : 0L;
        long outboxPendingAgeSeconds = outboxMetricsService != null ? outboxMetricsService.pendingOldestAgeSeconds() : 0L;
        boolean outboxLagExceeded = outboxProperties != null && outboxPendingAgeSeconds >= Math.max(0L, outboxProperties.getPendingLagAfter().getSeconds()) && outboxPending > 0L;
        return new SystemSnapshot(writePool, readPool, outboxPending, outboxPendingAgeSeconds, outboxLagExceeded);
    }

    public Map<String, Object> exportReadiness() {
        Map<String, Object> out = new LinkedHashMap<>();
        SystemSnapshot snapshot = snapshot();
        out.put("writePool", mapPool(snapshot.writePool()));
        out.put("readPool", mapPool(snapshot.readPool()));
        out.put("outboxPending", snapshot.outboxPending());
        out.put("outboxPendingAgeSeconds", snapshot.outboxPendingAgeSeconds());
        out.put("outboxPendingLagExceeded", snapshot.outboxPendingLagExceeded());
        PjbOperationalCrisisService crisisService = crisisServiceProvider.getIfAvailable();
        if (crisisService != null) {
            out.put("operationalCrisis", crisisService.exportState());
        }
        Map<String, Object> domains = new LinkedHashMap<>();
        for (PjbFunctionalDomain domain : PjbFunctionalDomain.values()) {
            FunctionalReadiness readiness = readiness(domain);
            domains.put(domain.externalName(), Map.of(
                    "available", readiness.available(),
                    "reason", readiness.reason()
            ));
        }
        out.put("domains", domains);
        return out;
    }

    private static DataSource choose(DataSource preferred, DataSource fallback) {
        return preferred != null ? preferred : fallback;
    }

    private static Map<String, Object> mapPool(PoolSnapshot pool) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("poolName", pool.poolName());
        out.put("active", pool.active());
        out.put("maximum", pool.maximum());
        out.put("awaiting", pool.awaiting());
        out.put("activeRatio", pool.activeRatio());
        return out;
    }

    public record FunctionalReadiness(PjbFunctionalDomain domain, boolean available, String reason, SystemSnapshot snapshot) {
    }

    public record SystemSnapshot(PoolSnapshot writePool,
                                 PoolSnapshot readPool,
                                 long outboxPending,
                                 long outboxPendingAgeSeconds,
                                 boolean outboxPendingLagExceeded) {
    }
}
