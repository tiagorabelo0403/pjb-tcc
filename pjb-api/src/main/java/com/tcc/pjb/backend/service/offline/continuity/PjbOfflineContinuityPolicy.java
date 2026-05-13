package com.tcc.pjb.backend.service.offline.continuity;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class PjbOfflineContinuityPolicy {

    public PjbOfflineContinuityDecision decide(PjbOfflineContinuityRequest request) {
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> allowed = new LinkedHashSet<>();
        if (request == null || Objects.toString(request.processNumber(), "").isBlank()) {
            blockers.add("PROCESSO_NAO_IDENTIFICADO");
        }
        if (request == null || Objects.toString(request.deviceFingerprint(), "").isBlank()) {
            blockers.add("DISPOSITIVO_NAO_VINCULADO");
        }
        if (request == null || !request.sealedLocalVault()) {
            blockers.add("COFRE_LOCAL_NAO_SELADO");
        }
        if (request == null || !request.latestSnapshotAvailable()) {
            blockers.add("SNAPSHOT_ATUAL_AUSENTE");
        }
        if (request != null && request.hasSensitiveSecrecy()) {
            blockers.add("SIGILO_EXIGE_CANAL_ONLINE");
        }
        if (staleCapture(request)) {
            blockers.add("CAPTURA_OFFLINE_EXPIRADA");
        }
        Set<PjbOfflineContinuityActionKind> actions = request == null || request.requestedActions() == null ? Set.of() : request.requestedActions();
        for (PjbOfflineContinuityActionKind action : actions) {
            if (action == PjbOfflineContinuityActionKind.CONSULTA || action == PjbOfflineContinuityActionKind.MINUTA || action == PjbOfflineContinuityActionKind.JUNTADA) {
                allowed.add(action.name());
            }
        }
        boolean restricted = actions.stream().anyMatch(action -> action == PjbOfflineContinuityActionKind.ASSINATURA || action == PjbOfflineContinuityActionKind.DECISAO || action == PjbOfflineContinuityActionKind.PROTOCOLO);
        if (restricted) {
            blockers.add("ATO_CRITICO_EXIGE_REVALIDACAO_ONLINE");
        }
        boolean review = request != null && request.conflictDetected() || restricted;
        boolean allowedOffline = blockers.isEmpty() && !allowed.isEmpty();
        String status = allowedOffline ? review ? "OFFLINE_WITH_REPLAY_REVIEW" : "OFFLINE_ALLOWED" : "ONLINE_REQUIRED";
        return new PjbOfflineContinuityDecision(status, allowedOffline, review, new ArrayList<>(allowed), new ArrayList<>(blockers));
    }

    private boolean staleCapture(PjbOfflineContinuityRequest request) {
        return request == null || request.capturedAt() == null || Duration.between(request.capturedAt(), Instant.now()).toHours() > 72;
    }
}
