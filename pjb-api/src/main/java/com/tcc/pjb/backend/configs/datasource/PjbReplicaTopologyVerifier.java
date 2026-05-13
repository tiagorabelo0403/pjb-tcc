package com.tcc.pjb.backend.configs.datasource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pjb.datasource.routing", name = "enabled", havingValue = "true")
public class PjbReplicaTopologyVerifier implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PjbReplicaTopologyVerifier.class);

    private final PjbDataSourceRoutingProperties properties;
    private final DataSource writeDataSource;
    private final DataSource readDataSource;
    private final Map<String, DataSource> regionalReadDataSources;

    public PjbReplicaTopologyVerifier(PjbDataSourceRoutingProperties properties,
                                      @Qualifier("pjbWriteDataSource") DataSource writeDataSource,
                                      @Qualifier("pjbReadDataSource") DataSource readDataSource,
                                      @Qualifier("pjbRegionalReadReplicaDataSources") Map<String, DataSource> regionalReadDataSources) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.writeDataSource = Objects.requireNonNull(writeDataSource, "writeDataSource");
        this.readDataSource = Objects.requireNonNull(readDataSource, "readDataSource");
        this.regionalReadDataSources = Map.copyOf(Objects.requireNonNullElse(regionalReadDataSources, Map.of()));
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!properties.isVerifyTopologyOnStartup()) {
            return;
        }
        TopologyProbe writeProbe = probe("write", writeDataSource);
        TopologyProbe readProbe = probe("read", readDataSource);
        validateOrThrow(writeProbe, readProbe, "Datasource de leitura padrão");
        for (Map.Entry<String, DataSource> entry : regionalReadDataSources.entrySet()) {
            TopologyProbe regionalProbe = probe(entry.getKey(), entry.getValue());
            validateOrThrow(writeProbe, regionalProbe, "Datasource regional " + entry.getKey());
        }
        LOGGER.info("Replica topology verificada com sucesso: writeUrl={}, readUrl={}, regionais={}",
                writeProbe.url(), readProbe.url(), regionalReadDataSources.keySet());
    }

    private void validateOrThrow(TopologyProbe writeProbe, TopologyProbe readProbe, String label) {
        String violation = validate(writeProbe, readProbe);
        if (violation != null) {
            String message = label + ": " + violation;
            if (properties.isStrict()) {
                throw new IllegalStateException(message);
            }
            LOGGER.warn(message);
        }
    }

    private String validate(TopologyProbe writeProbe, TopologyProbe readProbe) {
        if (!writeProbe.postgres() || !readProbe.postgres()) {
            return null;
        }
        if (sameJdbcUrl(writeProbe.url(), readProbe.url())) {
            return "aponta para o mesmo endpoint do datasource de escrita";
        }
        if (Boolean.TRUE.equals(writeProbe.inRecovery())) {
            return "escrita conectado a instância PostgreSQL em recuperação";
        }
        if (!Boolean.TRUE.equals(readProbe.inRecovery())) {
            return "não confirmou réplica PostgreSQL física via pg_is_in_recovery()";
        }
        return null;
    }

    private TopologyProbe probe(String role, DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String url = metaData == null ? null : metaData.getURL();
            String product = metaData == null ? null : metaData.getDatabaseProductName();
            boolean postgres = product != null && product.toLowerCase().contains("postgres");
            Boolean inRecovery = postgres ? queryBoolean(connection, "select pg_is_in_recovery()") : null;
            LOGGER.info("Datasource {} verificado: url={}, postgres={}, inRecovery={}", role, url, postgres, inRecovery);
            return new TopologyProbe(role, url, postgres, inRecovery);
        }
    }

    private Boolean queryBoolean(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getBoolean(1);
            }
            return null;
        }
    }

    private boolean sameJdbcUrl(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    private record TopologyProbe(String role, String url, boolean postgres, Boolean inRecovery) {
    }
}
