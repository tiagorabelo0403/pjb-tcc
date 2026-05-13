package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class PjbReplicaFallbackDataSourceTest {

    @Test
    void shouldUseReplicaWhenAvailable() throws Exception {
        DataSource read = mock(DataSource.class);
        DataSource write = mock(DataSource.class);
        Connection replicaConnection = mock(Connection.class);
        when(read.getConnection()).thenReturn(replicaConnection);

        PjbReplicaFailoverTracker tracker = new PjbReplicaFailoverTracker(java.time.Duration.ofSeconds(30), true, null);
        PjbReplicaFallbackDataSource dataSource = new PjbReplicaFallbackDataSource(read, write, tracker);

        Connection connection = dataSource.getConnection();

        assertThat(connection).isSameAs(replicaConnection);
        assertThat(tracker.isReplicaAvailable()).isTrue();
        assertThat(tracker.readSuccessCount()).isEqualTo(1L);
        assertThat(tracker.readFailureCount()).isZero();
    }

    @Test
    void shouldFallbackToWriteWhenReplicaFails() throws Exception {
        DataSource read = mock(DataSource.class);
        DataSource write = mock(DataSource.class);
        Connection primaryConnection = mock(Connection.class);
        when(read.getConnection()).thenThrow(new SQLException("replica down"));
        when(write.getConnection()).thenReturn(primaryConnection);

        PjbReplicaFailoverTracker tracker = new PjbReplicaFailoverTracker(java.time.Duration.ofSeconds(30), true, null);
        PjbReplicaFallbackDataSource dataSource = new PjbReplicaFallbackDataSource(read, write, tracker);

        Connection connection = dataSource.getConnection();

        assertThat(connection).isSameAs(primaryConnection);
        assertThat(tracker.isReplicaAvailable()).isFalse();
        assertThat(tracker.readFailureCount()).isEqualTo(1L);
        assertThat(tracker.fallbackCount()).isEqualTo(1L);
    }
}
