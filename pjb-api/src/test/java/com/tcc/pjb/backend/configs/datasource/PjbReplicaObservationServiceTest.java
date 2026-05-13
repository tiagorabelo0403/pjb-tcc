package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class PjbReplicaObservationServiceTest {

    @Test
    void shouldCaptureReplayLagAndRegionalAvailability() throws Exception {
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        DataSource write = postgresDataSource("jdbc:postgresql://postgres-primary:5432/pjb", false, null);
        DataSource read = postgresDataSource("jdbc:postgresql://postgres-replica:5432/pjb", true, 2.5d);
        DataSource regional = postgresDataSource("jdbc:postgresql://postgres-ne:5432/pjb", true, 1.25d);
        PjbReplicaObservationService service = new PjbReplicaObservationService(
                properties,
                write,
                read,
                Map.of("READ_NORDESTE", regional),
                new PjbReplicaFailoverTracker(java.time.Duration.ofSeconds(30), true, null)
        );

        PjbReplicaObservationService.ReplicaObservationSnapshot snapshot = service.currentSnapshot();

        assertThat(snapshot.read().available()).isTrue();
        assertThat(snapshot.read().replayLagSeconds()).isEqualTo(2.5d);
        assertThat(snapshot.availableRegionalReplicas()).isEqualTo(1L);
        assertThat(snapshot.regional()).containsKey("READ_NORDESTE");
        assertThat(snapshot.regional().get("READ_NORDESTE").replayLagSeconds()).isEqualTo(1.25d);
    }

    @Test
    void shouldMarkReplicaUnavailableWhenConnectionFails() throws Exception {
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        DataSource write = postgresDataSource("jdbc:postgresql://postgres-primary:5432/pjb", false, null);
        DataSource read = mock(DataSource.class);
        when(read.getConnection()).thenThrow(new java.sql.SQLException("replica offline"));
        PjbReplicaObservationService service = new PjbReplicaObservationService(
                properties,
                write,
                read,
                Map.of(),
                new PjbReplicaFailoverTracker(java.time.Duration.ofSeconds(30), true, null)
        );

        PjbReplicaObservationService.ReplicaObservationSnapshot snapshot = service.currentSnapshot();

        assertThat(snapshot.read().available()).isFalse();
        assertThat(snapshot.read().error()).contains("replica offline");
    }

    private DataSource postgresDataSource(String url, boolean inRecovery, Double lagSeconds) throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        PreparedStatement recoveryStatement = mock(PreparedStatement.class);
        ResultSet recoveryResultSet = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getURL()).thenReturn(url);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.prepareStatement("select pg_is_in_recovery()")).thenReturn(recoveryStatement);
        when(recoveryStatement.executeQuery()).thenReturn(recoveryResultSet);
        when(recoveryResultSet.next()).thenReturn(true);
        when(recoveryResultSet.getBoolean(1)).thenReturn(inRecovery);
        if (lagSeconds != null) {
            PreparedStatement lagStatement = mock(PreparedStatement.class);
            ResultSet lagResultSet = mock(ResultSet.class);
            when(connection.prepareStatement("select extract(epoch from coalesce(now() - pg_last_xact_replay_timestamp(), interval '0 seconds'))"))
                    .thenReturn(lagStatement);
            when(lagStatement.executeQuery()).thenReturn(lagResultSet);
            when(lagResultSet.next()).thenReturn(true);
            when(lagResultSet.getDouble(1)).thenReturn(lagSeconds);
        }
        return dataSource;
    }
}
