package com.tcc.pjb.backend.service.julgamento.safety;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.julgamento.DecisionFocusSession;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;

@Service
public class DecisionClientBindingGuardService {

    private static final Duration HEARTBEAT_MAX_AGE = Duration.ofSeconds(45);

    public void assertBoundActiveClient(DecisionFocusSession session, Long processoId) {
        if (session == null) {
            throw validation("Sessão decisional inexistente para conferência de aba ativa.");
        }
        if (session.getLastHeartbeatAt() == null || session.getLastHeartbeatAt().isBefore(Instant.now().minus(HEARTBEAT_MAX_AGE))) {
            throw validation("A aba ativa do processo não confirmou presença recente. Reabra a tela correta antes de decidir.")
                    .addMetadado("processo_id", processoId)
                    .addMetadado("session_id", session.getId());
        }
        String currentWindow = RequestContext.getClientWindowBinding().orElse(null);
        String currentTab = RequestContext.getClientTabBinding().orElse(null);
        String currentRoute = RequestContext.getClientRouteBinding().orElse(null);
        assertMatch("janela", session.getWindowBinding(), currentWindow, processoId, session.getId());
        assertMatch("aba", session.getTabBinding(), currentTab, processoId, session.getId());
        if (notBlank(session.getRouteBinding())) {
            if (!notBlank(currentRoute)) {
                throw validation("A rota ativa do cliente não foi informada para conferência de julgamento seguro.")
                        .addMetadado("processo_id", processoId)
                        .addMetadado("session_id", session.getId());
            }
            String normalizedExpected = normalizeRoute(session.getRouteBinding());
            String normalizedCurrent = normalizeRoute(currentRoute);
            if (!normalizedCurrent.equals(normalizedExpected) && !normalizedCurrent.contains("/processos/" + processoId)) {
                throw validation("A rota ativa informada pelo cliente não corresponde ao processo em foco decisional.")
                        .addMetadado("processo_id", processoId)
                        .addMetadado("session_id", session.getId())
                        .addMetadado("route_expected", normalizedExpected)
                        .addMetadado("route_current", normalizedCurrent);
            }
        }
    }

    public String computeBindingFingerprint(String windowBinding, String tabBinding, String routeBinding) {
        return Hashes.sha256Hex(safe(windowBinding) + "|" + safe(tabBinding) + "|" + safe(normalizeRoute(routeBinding)));
    }

    private void assertMatch(String label, String expected, String current, Long processoId, Long sessionId) {
        if (!notBlank(expected)) {
            return;
        }
        if (!notBlank(current)) {
            throw validation("O binding ativo de " + label + " não foi informado pelo cliente.")
                    .addMetadado("processo_id", processoId)
                    .addMetadado("session_id", sessionId)
                    .addMetadado("binding_type", label);
        }
        if (!expected.trim().equals(current.trim())) {
            throw validation("O binding ativo de " + label + " diverge do foco decisional armado.")
                    .addMetadado("processo_id", processoId)
                    .addMetadado("session_id", sessionId)
                    .addMetadado("binding_type", label);
        }
    }

    private String normalizeRoute(String route) {
        if (!notBlank(route)) {
            return null;
        }
        String sanitized = route.trim();
        int queryIndex = sanitized.indexOf('?');
        if (queryIndex >= 0) {
            sanitized = sanitized.substring(0, queryIndex);
        }
        return sanitized.toLowerCase(Locale.ROOT);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "-" : value.trim();
    }

    private ErroDeValidacaoException validation(String detail) {
        return new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, detail);
    }
}
