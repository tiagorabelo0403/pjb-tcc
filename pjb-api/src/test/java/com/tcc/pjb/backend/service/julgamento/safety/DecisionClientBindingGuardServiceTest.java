package com.tcc.pjb.backend.service.julgamento.safety;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.model.entity.julgamento.DecisionFocusSession;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;

class DecisionClientBindingGuardServiceTest {

    private final DecisionClientBindingGuardService service = new DecisionClientBindingGuardService();

    @Test
    void accepts_same_window_tab_and_recent_heartbeat() {
        DecisionFocusSession session = session();
        RequestContext.run("req-1", () -> {
            RequestContext.setClientWindowBinding("win-1");
            RequestContext.setClientTabBinding("tab-1");
            RequestContext.setClientRouteBinding("/app/processos/77/julgamento");
            assertDoesNotThrow(() -> service.assertBoundActiveClient(session, 77L));
        });
    }

    @Test
    void rejects_tab_mismatch() {
        DecisionFocusSession session = session();
        RequestContext.run("req-2", () -> {
            RequestContext.setClientWindowBinding("win-1");
            RequestContext.setClientTabBinding("tab-outra");
            RequestContext.setClientRouteBinding("/app/processos/77/julgamento");
            assertThrows(ErroDeValidacaoException.class, () -> service.assertBoundActiveClient(session, 77L));
        });
    }

    @Test
    void rejects_stale_heartbeat() {
        DecisionFocusSession session = session();
        session.setLastHeartbeatAt(Instant.now().minusSeconds(120));
        RequestContext.run("req-3", () -> {
            RequestContext.setClientWindowBinding("win-1");
            RequestContext.setClientTabBinding("tab-1");
            RequestContext.setClientRouteBinding("/app/processos/77/julgamento");
            assertThrows(ErroDeValidacaoException.class, () -> service.assertBoundActiveClient(session, 77L));
        });
    }

    private DecisionFocusSession session() {
        DecisionFocusSession session = new DecisionFocusSession();
        session.setId(99L);
        session.setWindowBinding("win-1");
        session.setTabBinding("tab-1");
        session.setRouteBinding("/app/processos/77/julgamento");
        session.setLastHeartbeatAt(Instant.now());
        return session;
    }
}
