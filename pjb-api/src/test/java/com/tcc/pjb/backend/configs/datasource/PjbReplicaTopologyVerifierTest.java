package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class PjbReplicaTopologyVerifierTest {

    @Test
    void shouldAcceptPrimaryAndReplicaTopology() throws Exception {
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        DataSource write = postgresDataSource("jdbc:postgresql://postgres-primary:5432/pjb", false);
        DataSource read = postgresDataSource("jdbc:postgresql://postgres-replica:5432/pjb", true);
        PjbReplicaTopologyVerifier verifier = new PjbReplicaTopologyVerifier(properties, write, read, Map.of());

        assertThatCode(() -> verifier.run(new DefaultApplicationArguments(new String[0]))).doesNotThrowAnyException();
    }

    @Test
    void shouldFailWhenReadReplicaPointsToPrimary() throws Exception {
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        DataSource write = postgresDataSource("jdbc:postgresql://postgres-primary:5432/pjb", false);
        DataSource read = postgresDataSource("jdbc:postgresql://postgres-primary:5432/pjb", false);
        PjbReplicaTopologyVerifier verifier = new PjbReplicaTopologyVerifier(properties, write, read, Map.of());

        assertThatThrownBy(() -> verifier.run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mesmo endpoint");
    }

    @Test
    void shouldFailWhenRegionalReplicaPointsToPrimary() throws Exception {
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        DataSource write = postgresDataSource("jdbc:postgresql://postgres-primary:5432/pjb", false);
        DataSource read = postgresDataSource("jdbc:postgresql://postgres-replica:5432/pjb", true);
        DataSource regional = postgresDataSource("jdbc:postgresql://postgres-primary:5432/pjb", false);
        PjbReplicaTopologyVerifier verifier = new PjbReplicaTopologyVerifier(properties, write, read, Map.of("READ_NORDESTE", regional));

        assertThatThrownBy(() -> verifier.run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("regional READ_NORDESTE");
    }

    private DataSource postgresDataSource(String url, boolean inRecovery) throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getURL()).thenReturn(url);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.prepareStatement("select pg_is_in_recovery()"))
                .thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBoolean(1)).thenReturn(inRecovery);
        return dataSource;
    }
}
