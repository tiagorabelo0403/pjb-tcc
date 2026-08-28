package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.lgpd.ProcessoSigiloRlsEnvelope;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class PjbProcessoSigiloRlsDataSourceTest {

    @Test
    void deveAplicarEResetarVariaveisDeSessaoNoCheckoutEDisposeDaConexao() throws Exception {
        DataSource delegate = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        PreparedStatement applyStatement = mock(PreparedStatement.class);
        PreparedStatement resetStatement = mock(PreparedStatement.class);
        when(delegate.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.prepareStatement(anyString())).thenReturn(applyStatement, resetStatement);

        PjbProcessoSigiloRlsContext context = new PjbProcessoSigiloRlsContext();
        context.bind(new ProcessoSigiloRlsEnvelope(
                52L,
                "tb_processo",
                "SIGILO_N2",
                "SIGILO_N2",
                "UNID-9",
                "TJCE",
                "PROC_SIGILO|TJCE|UNID-9|SIGILO_N2",
                true,
                true,
                true,
                false,
                true,
                List.of("DADOS_JUDICIAIS"),
                List.of(),
                null,
                null
        ));
        PjbRlsActorResolver actorResolver = mock(PjbRlsActorResolver.class);
        when(actorResolver.currentOrAnonymous()).thenReturn(PjbRlsActorResolver.ANONYMOUS);
        PjbRlsEquipeResolver equipeResolver = mock(PjbRlsEquipeResolver.class);
        when(equipeResolver.currentOrInactive()).thenReturn(PjbRlsEquipeResolver.INACTIVE);
        PjbProcessoSigiloRlsDataSource dataSource = new PjbProcessoSigiloRlsDataSource(delegate, context, actorResolver, equipeResolver);

        Connection proxied = dataSource.getConnection();
        proxied.close();

        InOrder order = inOrder(connection, applyStatement, resetStatement);
        order.verify(connection).prepareStatement(anyString());
        order.verify(applyStatement).setString(1, "SIGILO_N2");
        order.verify(applyStatement).setString(2, "TJCE");
        order.verify(applyStatement).setString(3, "UNID-9");
        order.verify(applyStatement).setString(4, "PROC_SIGILO|TJCE|UNID-9|SIGILO_N2");
        order.verify(applyStatement).setString(5, "");
        order.verify(applyStatement).setString(6, "");
        order.verify(applyStatement).setString(7, "false");
        order.verify(applyStatement).setString(8, "");
        order.verify(applyStatement).setString(9, "");
        order.verify(applyStatement).execute();
        order.verify(applyStatement).close();
        order.verify(connection).prepareStatement(anyString());
        order.verify(resetStatement).setString(1, "PUBLICO");
        order.verify(resetStatement).setString(2, "");
        order.verify(resetStatement).setString(3, "");
        order.verify(resetStatement).setString(4, "");
        order.verify(resetStatement).setString(5, "");
        order.verify(resetStatement).setString(6, "");
        order.verify(resetStatement).setString(7, "false");
        order.verify(resetStatement).setString(8, "");
        order.verify(resetStatement).setString(9, "");
        order.verify(resetStatement).execute();
        order.verify(resetStatement).close();
        verify(connection).close();
        assertThat(proxied.isWrapperFor(Connection.class)).isTrue();
        assertThat(proxied.unwrap(Connection.class)).isSameAs(connection);
    }

    @Test
    void bancoNaoPostgres_naoTentaAplicarNemResetarGucs() throws Exception {
        DataSource delegate = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(delegate.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("H2");

        PjbProcessoSigiloRlsContext context = new PjbProcessoSigiloRlsContext();
        PjbRlsActorResolver actorResolver = mock(PjbRlsActorResolver.class);
        when(actorResolver.currentOrAnonymous()).thenReturn(PjbRlsActorResolver.ANONYMOUS);
        PjbRlsEquipeResolver equipeResolver = mock(PjbRlsEquipeResolver.class);
        when(equipeResolver.currentOrInactive()).thenReturn(PjbRlsEquipeResolver.INACTIVE);
        PjbProcessoSigiloRlsDataSource dataSource = new PjbProcessoSigiloRlsDataSource(delegate, context, actorResolver, equipeResolver);

        Connection proxied = dataSource.getConnection();
        proxied.close();

        verify(connection, org.mockito.Mockito.never()).prepareStatement(anyString());
        verify(connection).close();
    }
}
