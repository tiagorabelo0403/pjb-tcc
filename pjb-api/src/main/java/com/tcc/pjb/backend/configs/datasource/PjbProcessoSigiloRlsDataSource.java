package com.tcc.pjb.backend.configs.datasource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.AbstractDataSource;

public class PjbProcessoSigiloRlsDataSource extends AbstractDataSource {

    private static final String APPLY_SQL = "select set_config('app.pjb_sigilo_clearance', ?, false), "
            + "set_config('app.pjb_tribunal_code', ?, false), "
            + "set_config('app.pjb_unit_code', ?, false), "
            + "set_config('app.pjb_sigilo_scope', ?, false), "
            + "set_config('app.pjb_actor_id', ?, false), "
            + "set_config('app.pjb_actor_roles', ?, false)";

    private final DataSource delegate;
    private final PjbProcessoSigiloRlsContext context;
    private final PjbRlsActorResolver actorResolver;

    public PjbProcessoSigiloRlsDataSource(DataSource delegate,
                                          PjbProcessoSigiloRlsContext context,
                                          PjbRlsActorResolver actorResolver) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.context = Objects.requireNonNull(context, "context");
        this.actorResolver = Objects.requireNonNull(actorResolver, "actorResolver");
    }

    @Override
    public Connection getConnection() throws SQLException {
        return wrap(delegate.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return wrap(delegate.getConnection(username, password));
    }

    private Connection wrap(Connection connection) throws SQLException {
        PjbProcessoSigiloRlsContext.SessionSettings settings = context.currentOrDefault();
        PjbRlsActorResolver.ActorSettings actor = actorResolver.currentOrAnonymous();
        try {
            applySettings(connection, settings, actor);
        } catch (SQLException ex) {
            try {
                connection.close();
            } catch (SQLException closeEx) {
                ex.addSuppressed(closeEx);
            }
            throw ex;
        }
        InvocationHandler handler = new ConnectionInvocationHandler(connection, settings);
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                handler
        );
    }

    private void applySettings(Connection connection,
                               PjbProcessoSigiloRlsContext.SessionSettings settings,
                               PjbRlsActorResolver.ActorSettings actor) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(APPLY_SQL)) {
            statement.setString(1, settings.sigiloClearance());
            statement.setString(2, settings.tribunalCode());
            statement.setString(3, settings.unitCode());
            statement.setString(4, settings.sigiloScope());
            statement.setString(5, actor.actorId());
            statement.setString(6, actor.roles());
            statement.execute();
        }
    }

    private final class ConnectionInvocationHandler implements InvocationHandler {

        private final Connection delegateConnection;
        private final PjbProcessoSigiloRlsContext.SessionSettings settings;
        private boolean returned;

        private ConnectionInvocationHandler(Connection delegateConnection,
                                            PjbProcessoSigiloRlsContext.SessionSettings settings) {
            this.delegateConnection = delegateConnection;
            this.settings = settings;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (name.equals("close")) {
                return handleClose();
            }
            if (name.equals("unwrap") && args != null && args.length == 1 && args[0] instanceof Class<?> type) {
                if (type.isInstance(delegateConnection) || type == Connection.class) {
                    return delegateConnection;
                }
            }
            if (name.equals("isWrapperFor") && args != null && args.length == 1 && args[0] instanceof Class<?> type) {
                return type.isInstance(delegateConnection) || type == Connection.class;
            }
            if (name.equals("toString")) {
                return "PjbProcessoSigiloRlsConnection[settings=" + settings + "]";
            }
            try {
                return method.invoke(delegateConnection, args);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }

        private Object handleClose() throws SQLException {
            if (returned) {
                return null;
            }
            returned = true;
            SQLException failure = null;
            try {
                applySettings(delegateConnection, PjbProcessoSigiloRlsContext.DEFAULT_SETTINGS, PjbRlsActorResolver.ANONYMOUS);
            } catch (SQLException ex) {
                failure = ex;
            }
            try {
                delegateConnection.close();
            } catch (SQLException ex) {
                if (failure != null) {
                    failure.addSuppressed(ex);
                    throw failure;
                }
                throw ex;
            }
            if (failure != null) {
                throw failure;
            }
            return null;
        }
    }
}
