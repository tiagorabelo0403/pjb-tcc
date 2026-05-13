package com.tcc.pjb.backend.configs.datasource;

import com.zaxxer.hikari.HikariDataSource;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.sql.DataSource;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

@Configuration
@ConditionalOnProperty(prefix = "pjb.datasource.routing", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(PjbDataSourceRoutingProperties.class)
public class PjbReadWriteDataSourceConfig {

    @Bean(name = "pjbWriteDataSource")
    @org.springframework.boot.context.properties.ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource pjbWriteDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "pjbReadDataSource")
    public HikariDataSource pjbReadDataSource(DataSourceProperties writeProperties,
                                              PjbDataSourceRoutingProperties routingProperties) {
        validateDefaultReplicaConfiguration(routingProperties);
        return buildReplicaDataSource("pjb-read-replica", writeProperties, routingProperties.getReplica(), false);
    }

    @Bean(name = "pjbRegionalReadReplicaDataSources")
    public Map<String, DataSource> pjbRegionalReadReplicaDataSources(DataSourceProperties writeProperties,
                                                                     PjbDataSourceRoutingProperties routingProperties) {
        Map<String, DataSource> out = new LinkedHashMap<>();
        routingProperties.getRegionalReplicas().forEach((region, replica) -> {
            if (region == null || region.isBlank() || replica == null || isBlank(replica.getUrl())) {
                return;
            }
            String normalizedRegion = region.trim().toUpperCase(Locale.ROOT);
            String key = "READ_" + normalizedRegion;
            out.put(key, buildReplicaDataSource("pjb-read-" + normalizedRegion.toLowerCase(Locale.ROOT), writeProperties, replica, true));
        });
        return out;
    }


    @Bean(name = "pjbWriteFailoverCandidateDataSources")
    public Map<String, DataSource> pjbWriteFailoverCandidateDataSources(DataSourceProperties writeProperties,
                                                                        PjbDataSourceRoutingProperties routingProperties) {
        Map<String, DataSource> out = new LinkedHashMap<>();
        PjbDataSourceRoutingProperties.WriteFailover writeFailover = routingProperties.getWriteFailover();
        writeFailover.getCandidates().forEach((name, candidate) -> {
            if (name == null || name.isBlank() || candidate == null || isBlank(candidate.getUrl())) {
                return;
            }
            String normalizedName = name.trim().toUpperCase(Locale.ROOT);
            HikariDataSource dataSource = buildReplicaDataSource("pjb-write-" + normalizedName.toLowerCase(Locale.ROOT), writeProperties, candidate, false);
            dataSource.setReadOnly(false);
            out.put(normalizedName, dataSource);
        });
        return out;
    }

    @Bean
    public PjbWriteFailoverTracker pjbWriteFailoverTracker(PjbDataSourceRoutingProperties routingProperties,
                                                           org.springframework.beans.factory.ObjectProvider<MeterRegistry> meterRegistryProvider) {
        PjbDataSourceRoutingProperties.WriteFailover writeFailover = routingProperties.getWriteFailover();
        return new PjbWriteFailoverTracker(writeFailover.getFailureCooldown(), writeFailover.getSuccessStickiness(), meterRegistryProvider.getIfAvailable());
    }

    @Bean(name = "pjbWriteRoutingDataSource")
    public DataSource pjbWriteRoutingDataSource(@Qualifier("pjbWriteDataSource") HikariDataSource writeDataSource,
                                                @Qualifier("pjbWriteFailoverCandidateDataSources") Map<String, DataSource> candidates,
                                                PjbWriteFailoverTracker tracker,
                                                PjbDataSourceRoutingProperties routingProperties) {
        if (!routingProperties.getWriteFailover().isEnabled() || candidates.isEmpty()) {
            return writeDataSource;
        }
        return new PjbWriteFailoverDataSource("PRIMARY", writeDataSource, candidates, tracker, routingProperties.getWriteFailover().isStrict());
    }

    @Bean
    public PjbReplicaFailoverTracker pjbReplicaFailoverTracker(PjbDataSourceRoutingProperties routingProperties,
                                                               org.springframework.beans.factory.ObjectProvider<MeterRegistry> meterRegistryProvider) {
        PjbDataSourceRoutingProperties.Replica replica = routingProperties.getReplica();
        return new PjbReplicaFailoverTracker(replica.getFailureCooldown(), replica.isFallbackToWriteOnError(), meterRegistryProvider.getIfAvailable());
    }

    @Bean(name = "pjbReadReplicaDataSource")
    public DataSource pjbReadReplicaDataSource(@Qualifier("pjbReadDataSource") HikariDataSource readDataSource,
                                               @Qualifier("pjbWriteRoutingDataSource") DataSource writeDataSource,
                                               PjbReplicaFailoverTracker tracker) {
        return new PjbReplicaFallbackDataSource(readDataSource, writeDataSource, tracker);
    }

    @Bean
    public PjbReplicaRoutingHealthIndicator pjbReplicaRoutingHealthIndicator(PjbReplicaFailoverTracker tracker,
                                                                             PjbReplicaObservationService observationService) {
        return new PjbReplicaRoutingHealthIndicator(tracker, observationService);
    }

    @Bean
    @Primary
    public DataSource dataSource(@Qualifier("pjbWriteRoutingDataSource") DataSource writeDataSource,
                                 @Qualifier("pjbReadReplicaDataSource") DataSource readDataSource,
                                 @Qualifier("pjbRegionalReadReplicaDataSources") Map<String, DataSource> regionalReadDataSources,
                                 PjbPrimaryReadPreferenceContext primaryReadPreferenceContext,
                                 PjbAdaptiveDataPlaneContext adaptiveDataPlaneContext,
                                 PjbDataSourceRoutingProperties routingProperties,
                                 PjbProcessoSigiloRlsContext processoSigiloRlsContext) {
        Map<Object, Object> targets = new LinkedHashMap<>();
        targets.put(PjbDataSourceRole.WRITE, writeDataSource);
        targets.put(PjbDataSourceRole.READ, readDataSource);
        targets.putAll(regionalReadDataSources);
        PjbReadReplicaRoutingDataSource routing = new PjbReadReplicaRoutingDataSource(primaryReadPreferenceContext, regionalReadDataSources.keySet(), routingProperties, adaptiveDataPlaneContext);
        routing.setTargetDataSources(targets);
        routing.setDefaultTargetDataSource(writeDataSource);
        routing.afterPropertiesSet();
        DataSource sigiloAwareRouting = new PjbProcessoSigiloRlsDataSource(routing, processoSigiloRlsContext);
        return new LazyConnectionDataSourceProxy(sigiloAwareRouting);
    }

    private static HikariDataSource buildReplicaDataSource(String poolName,
                                                           DataSourceProperties writeProperties,
                                                           PjbDataSourceRoutingProperties.Replica replica,
                                                           boolean allowWriteFallbackConfiguration) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setPoolName(poolName);
        dataSource.setJdbcUrl(resolveReplicaJdbcUrl(replica, writeProperties, allowWriteFallbackConfiguration));
        dataSource.setUsername(firstNonBlank(replica.getUsername(), writeProperties.getUsername()));
        dataSource.setPassword(firstNonBlank(replica.getPassword(), writeProperties.getPassword()));
        dataSource.setDriverClassName(firstNonBlank(replica.getDriverClassName(), writeProperties.getDriverClassName()));
        dataSource.setMaximumPoolSize(Math.max(1, replica.getHikari().getMaximumPoolSize()));
        dataSource.setMinimumIdle(Math.max(0, replica.getHikari().getMinimumIdle()));
        dataSource.setConnectionTimeout(Math.max(250L, replica.getHikari().getConnectionTimeout()));
        dataSource.setValidationTimeout(Math.max(250L, replica.getHikari().getValidationTimeout()));
        dataSource.setMaxLifetime(Math.max(30000L, replica.getHikari().getMaxLifetime()));
        dataSource.setKeepaliveTime(Math.max(0L, replica.getHikari().getKeepaliveTime()));
        dataSource.setInitializationFailTimeout(replica.getHikari().getInitializationFailTimeout());
        dataSource.setLeakDetectionThreshold(Math.max(0L, replica.getHikari().getLeakDetectionThreshold()));
        dataSource.setReadOnly(replica.getHikari().isReadOnly());
        dataSource.setRegisterMbeans(replica.getHikari().isRegisterMbeans());
        replica.getDataSourceProperties().forEach(dataSource::addDataSourceProperty);
        return dataSource;
    }

    private static void validateDefaultReplicaConfiguration(PjbDataSourceRoutingProperties routingProperties) {
        if (routingProperties.isStrict() && isBlank(routingProperties.getReplica().getUrl())) {
            throw new IllegalStateException("Read replica routing habilitado sem pjb.datasource.routing.replica.url configurado");
        }
    }

    private static String resolveReplicaJdbcUrl(PjbDataSourceRoutingProperties.Replica replica,
                                                DataSourceProperties writeProperties,
                                                boolean allowWriteFallbackConfiguration) {
        if (!isBlank(replica.getUrl())) {
            return replica.getUrl();
        }
        if (allowWriteFallbackConfiguration) {
            return writeProperties.getUrl();
        }
        throw new IllegalStateException("Datasource de leitura padrão sem URL configurada");
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
