package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

class PjbReadWriteDataSourceConfigTest {

    @Test
    void shouldFailFastWhenRoutingIsStrictAndReplicaUrlIsMissing() {
        PjbReadWriteDataSourceConfig config = new PjbReadWriteDataSourceConfig();
        DataSourceProperties writeProperties = new DataSourceProperties();
        writeProperties.setUrl("jdbc:postgresql://postgres:5432/pjb");
        writeProperties.setUsername("pjb");
        writeProperties.setPassword("pjb");
        writeProperties.setDriverClassName("org.postgresql.Driver");
        PjbDataSourceRoutingProperties routingProperties = new PjbDataSourceRoutingProperties();
        routingProperties.setStrict(true);

        assertThatThrownBy(() -> config.pjbReadDataSource(writeProperties, routingProperties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("replica.url");
    }
}
