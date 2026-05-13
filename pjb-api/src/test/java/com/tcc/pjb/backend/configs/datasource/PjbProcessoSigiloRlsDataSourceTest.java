package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.lgpd.ProcessoSigiloRlsEnvelope;
import java.sql.Connection;
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
        PreparedStatement applyStatement = mock(PreparedStatement.class);
        PreparedStatement resetStatement = mock(PreparedStatement.class);
        when(delegate.getConnection()).thenReturn(connection);
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
        PjbProcessoSigiloRlsDataSource dataSource = new PjbProcessoSigiloRlsDataSource(delegate, context);

        Connection proxied = dataSource.getConnection();
        proxied.close();

        InOrder order = inOrder(connection, applyStatement, resetStatement);
        order.verify(connection).prepareStatement(anyString());
        order.verify(applyStatement).setString(1, "SIGILO_N2");
        order.verify(applyStatement).setString(2, "TJCE");
        order.verify(applyStatement).setString(3, "UNID-9");
        order.verify(applyStatement).setString(4, "PROC_SIGILO|TJCE|UNID-9|SIGILO_N2");
        order.verify(applyStatement).execute();
        order.verify(applyStatement).close();
        order.verify(connection).prepareStatement(anyString());
        order.verify(resetStatement).setString(1, "PUBLICO");
        order.verify(resetStatement).setString(2, "");
        order.verify(resetStatement).setString(3, "");
        order.verify(resetStatement).setString(4, "");
        order.verify(resetStatement).execute();
        order.verify(resetStatement).close();
        verify(connection).close();
        assertThat(proxied.isWrapperFor(Connection.class)).isTrue();
        assertThat(proxied.unwrap(Connection.class)).isSameAs(connection);
    }
}
