package com.tcc.pjb.backend.service.criminal;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.criminal.PoliceExecutionReconciliationRequest;
import com.tcc.pjb.backend.model.dto.criminal.PoliceExecutionRetryRequest;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class PoliceTraceableExecutionLedgerService {

    private static final int DEFAULT_RETRY_LIMIT = 7;
    private static final int DEFAULT_LIST_LIMIT = 20;
    private static final int MAX_ACTIVE_EXECUTIONS = 20_000;
    private static final Duration ACTIVE_RETENTION = Duration.ofDays(3);
    private static final Duration TERMINAL_RETENTION = Duration.ofHours(12);
    private static final long CLEANUP_INTERVAL_NANOS = Duration.ofSeconds(30).toNanos();

    private final AuditLedgerService auditLedgerService;
    private final ConcurrentMap<String, TraceableExecutionState> states = new ConcurrentHashMap<>();
    private final AtomicLong nextCleanupAtNanos = new AtomicLong(System.nanoTime() + CLEANUP_INTERVAL_NANOS);

    public PoliceTraceableExecutionLedgerService(AuditLedgerService auditLedgerService) {
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService, "auditLedgerService");
    }

    public Map<String, Object> operationalLedgerBlueprint(TipoUsuario tipoUsuario) {
        return operationalLedgerBlueprint(actorLane(tipoUsuario));
    }

    public Map<String, Object> registerExecution(TipoUsuario tipoUsuario,
                                                 String operationCode,
                                                 Long inqueritoId,
                                                 Long processoId,
                                                 String partnerSystem,
                                                 String transactionFamily,
                                                 boolean nativeFirstStrict,
                                                 String priorityLane,
                                                 Map<String, Object> executionPlan,
                                                 List<String> nextActions,
                                                 List<String> partnerFallbackOrder) {
        Instant now = Instant.now();
        cleanupIfRequired(now, false);
        String actorLane = actorLane(tipoUsuario);
        String normalizedOperation = normalize(operationCode, "UNKNOWN_OPERATION");
        String normalizedPartnerSystem = normalize(partnerSystem, "PJB_NATIVE");
        String normalizedTransactionFamily = normalize(transactionFamily, normalizedOperation);
        Map<String, Object> safePlan = immutableMap(executionPlan);
        List<String> safeNextActions = immutableStrings(nextActions);
        List<String> safeFallbackOrder = immutableStrings(partnerFallbackOrder);
        String canonicalBase = String.join("|",
                actorLane,
                normalizedOperation,
                String.valueOf(inqueritoId),
                String.valueOf(processoId),
                normalizedPartnerSystem,
                normalizedTransactionFamily,
                String.valueOf(safePlan),
                String.valueOf(safeFallbackOrder));
        String routeHash = Hashes.sha256Hex(canonicalBase);
        String executionId = "PJB-POL-EXEC-" + Hashes.sha256HexPrefix(canonicalBase + ":execution", 18).toUpperCase(Locale.ROOT);
        String idempotencyKey = "police:execution:" + Hashes.sha256HexPrefix(canonicalBase + ":idempotency", 24);
        String initialStatus = normalizedOperation.equals("FILA_CONTINGENCIA_TRANSACIONAL") ? "REENTRADA_ENFILEIRADA" : "AGUARDANDO_CONFIRMACAO_EXTERNA";
        List<Map<String, Object>> trail = new ArrayList<>();
        trail.add(statusEvent("EXECUCAO_REGISTRADA", initialStatus, "Execução soberana registrada para rastreabilidade ponta a ponta.", routeHash, now, 0));
        if (initialStatus.equals("AGUARDANDO_CONFIRMACAO_EXTERNA")) {
            trail.add(statusEvent("CONFIRMACAO_PENDENTE", initialStatus, "Confirmação externa aguardando retorno do parceiro ou tribunal.", routeHash, now, 0));
        }
        String auditHash = computeAuditHash(executionId, initialStatus, 0, trail, safePlan, normalizedPartnerSystem);
        TraceableExecutionState state = new TraceableExecutionState(
                executionId,
                actorLane,
                normalizedOperation,
                inqueritoId,
                processoId,
                normalizedPartnerSystem,
                normalizedTransactionFamily,
                nativeFirstStrict,
                priorityLane == null || priorityLane.isBlank() ? "HIGH" : priorityLane.trim().toUpperCase(Locale.ROOT),
                initialStatus,
                0,
                DEFAULT_RETRY_LIMIT,
                now,
                now,
                null,
                queueName(actorLane, normalizedPartnerSystem, "CONFIRMATION"),
                queueName(actorLane, normalizedPartnerSystem, "ERROR"),
                queueName(actorLane, normalizedPartnerSystem, "RECONCILIATION"),
                idempotencyKey,
                routeHash,
                auditHash,
                safeFallbackOrder,
                safeNextActions,
                safePlan,
                Map.of(),
                List.copyOf(trail)
        );
        states.put(executionId, state);
        if (states.size() > MAX_ACTIVE_EXECUTIONS) {
            cleanupIfRequired(now, true);
        }
        auditLedgerService.appendSafely(
                "POLICE_NATIVE_EXECUTION_REGISTERED",
                "POLICE_EXECUTION",
                executionId,
                auditHash,
                normalizedOperation + "#" + normalizedPartnerSystem + "#" + state.currentStatus()
        );
        return snapshot(state);
    }

    public Map<String, Object> executionStatus(String executionId) {
        cleanupIfRequired(Instant.now(), false);
        TraceableExecutionState state = states.get(executionId);
        if (state == null) {
            return notFound(executionId);
        }
        return snapshot(state);
    }

    public Map<String, Object> recentExecutions(TipoUsuario tipoUsuario, int limit) {
        cleanupIfRequired(Instant.now(), false);
        String actorLane = actorLane(tipoUsuario);
        int safeLimit = limit <= 0 ? DEFAULT_LIST_LIMIT : Math.min(limit, 100);
        List<Map<String, Object>> executions = states.values().stream()
                .filter(state -> actorLane.equals(state.actorLane()))
                .sorted(Comparator.comparing(TraceableExecutionState::updatedAt).reversed())
                .limit(safeLimit)
                .map(this::snapshot)
                .toList();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "POLICE_TRACEABLE_EXECUTION_RECENT");
        out.put("actorLane", actorLane);
        out.put("limit", safeLimit);
        out.put("count", executions.size());
        out.put("executions", executions);
        out.put("operationalLedger", operationalLedgerBlueprint(actorLane));
        return immutableMap(out);
    }

    public Map<String, Object> reconcileExecution(TipoUsuario tipoUsuario,
                                                  String executionId,
                                                  PoliceExecutionReconciliationRequest request) {
        cleanupIfRequired(Instant.now(), false);
        TraceableExecutionState current = states.get(executionId);
        if (current == null) {
            return notFound(executionId);
        }
        String actorLane = actorLane(tipoUsuario);
        if (!actorLane.equals(current.actorLane())) {
            return accessMismatch(executionId, actorLane, current.actorLane());
        }
        PoliceExecutionReconciliationRequest safe = request == null
                ? new PoliceExecutionReconciliationRequest(current.partnerSystem(), "PENDENTE", null, null, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null)
                : request;
        Instant now = Instant.now();
        String normalizedPartnerSystem = normalize(safe.sistemaParceiro(), current.partnerSystem());
        String normalizedPartnerStatus = normalize(safe.statusParceiro(), "PENDENTE");
        String targetStatus = resolvePartnerStatus(normalizedPartnerStatus, safe.confirmarEntregaResolvida(), safe.reconciliarSnapshotResolvido());
        int attempts = current.attempts();
        Instant nextRetryAt = current.nextRetryAt();
        List<Map<String, Object>> trail = new ArrayList<>(current.statusTrail());
        LinkedHashMap<String, Object> partnerSignal = new LinkedHashMap<>();
        partnerSignal.put("partnerSystem", normalizedPartnerSystem);
        partnerSignal.put("partnerStatus", normalizedPartnerStatus);
        partnerSignal.put("protocol", safe.protocoloParceiro());
        partnerSignal.put("externalReference", safe.referenciaParceira());
        partnerSignal.put("confirmDelivery", safe.confirmarEntregaResolvida());
        partnerSignal.put("snapshotReconciled", safe.reconciliarSnapshotResolvido());
        partnerSignal.put("observation", safe.observacaoOperacional());
        partnerSignal.put("receivedAt", now.toString());
        trail.add(statusEvent("RECONCILIACAO_PARCEIRO", targetStatus, safe.observacaoOperacional(), current.routeHash(), now, attempts));
        if ((targetStatus.equals("ERRO_RECONCILIADO") || targetStatus.equals("ERRO_TRANSACIONAL"))
                && safe.acionarRetentativaResolvido()
                && attempts < current.retryLimit()) {
            attempts++;
            nextRetryAt = computeNextRetryAt(attempts);
            targetStatus = "REENTRADA_ENFILEIRADA";
            trail.add(statusEvent("RETENTATIVA_PROGRAMADA", targetStatus, "Retentativa programada após erro reconciliado com parceiro.", current.routeHash(), now, attempts));
        }
        String auditHash = computeAuditHash(executionId, targetStatus, attempts, trail, current.executionPlan(), normalizedPartnerSystem);
        TraceableExecutionState updated = new TraceableExecutionState(
                current.executionId(),
                current.actorLane(),
                current.operationCode(),
                current.inqueritoId(),
                current.processoId(),
                normalizedPartnerSystem,
                current.transactionFamily(),
                current.nativeFirstStrict(),
                current.priorityLane(),
                targetStatus,
                attempts,
                current.retryLimit(),
                current.createdAt(),
                now,
                nextRetryAt,
                queueName(current.actorLane(), normalizedPartnerSystem, "CONFIRMATION"),
                queueName(current.actorLane(), normalizedPartnerSystem, "ERROR"),
                queueName(current.actorLane(), normalizedPartnerSystem, "RECONCILIATION"),
                current.idempotencyKey(),
                current.routeHash(),
                auditHash,
                current.partnerFallbackOrder(),
                current.nextActions(),
                current.executionPlan(),
                Map.copyOf(partnerSignal),
                List.copyOf(trail)
        );
        states.put(executionId, updated);
        if (states.size() > MAX_ACTIVE_EXECUTIONS) {
            cleanupIfRequired(now, true);
        }
        auditLedgerService.appendSafely(
                "POLICE_NATIVE_EXECUTION_RECONCILED",
                "POLICE_EXECUTION",
                executionId,
                auditHash,
                normalizedPartnerSystem + "#" + normalizedPartnerStatus + "#" + targetStatus
        );
        return snapshot(updated);
    }

    public Map<String, Object> retryExecution(TipoUsuario tipoUsuario,
                                              String executionId,
                                              PoliceExecutionRetryRequest request) {
        cleanupIfRequired(Instant.now(), false);
        TraceableExecutionState current = states.get(executionId);
        if (current == null) {
            return notFound(executionId);
        }
        String actorLane = actorLane(tipoUsuario);
        if (!actorLane.equals(current.actorLane())) {
            return accessMismatch(executionId, actorLane, current.actorLane());
        }
        PoliceExecutionRetryRequest safe = request == null
                ? new PoliceExecutionRetryRequest(current.partnerSystem(), null, null, current.nativeFirstStrict())
                : request;
        Instant now = Instant.now();
        int attempts = Math.max(current.attempts() + 1, safe.tentativaForcadaResolvida());
        Instant nextRetryAt = computeNextRetryAt(attempts);
        String normalizedPartnerSystem = normalize(safe.sistemaParceiro(), current.partnerSystem());
        List<Map<String, Object>> trail = new ArrayList<>(current.statusTrail());
        trail.add(statusEvent("RETENTATIVA_MANUAL_ENFILEIRADA", "REENTRADA_ENFILEIRADA", safe.motivoOperacional(), current.routeHash(), now, attempts));
        String auditHash = computeAuditHash(executionId, "REENTRADA_ENFILEIRADA", attempts, trail, current.executionPlan(), normalizedPartnerSystem);
        TraceableExecutionState updated = new TraceableExecutionState(
                current.executionId(),
                current.actorLane(),
                current.operationCode(),
                current.inqueritoId(),
                current.processoId(),
                normalizedPartnerSystem,
                current.transactionFamily(),
                safe.manterNativeFirstEstritoResolvido(),
                current.priorityLane(),
                "REENTRADA_ENFILEIRADA",
                attempts,
                current.retryLimit(),
                current.createdAt(),
                now,
                nextRetryAt,
                queueName(current.actorLane(), normalizedPartnerSystem, "CONFIRMATION"),
                queueName(current.actorLane(), normalizedPartnerSystem, "ERROR"),
                queueName(current.actorLane(), normalizedPartnerSystem, "RECONCILIATION"),
                current.idempotencyKey(),
                current.routeHash(),
                auditHash,
                current.partnerFallbackOrder(),
                current.nextActions(),
                current.executionPlan(),
                Map.of(
                        "partnerSystem", normalizedPartnerSystem,
                        "reason", safe.motivoOperacional(),
                        "queuedAt", now.toString(),
                        "forcedAttempt", attempts,
                        "manual", Boolean.TRUE
                ),
                List.copyOf(trail)
        );
        states.put(executionId, updated);
        if (states.size() > MAX_ACTIVE_EXECUTIONS) {
            cleanupIfRequired(now, true);
        }
        auditLedgerService.appendSafely(
                "POLICE_NATIVE_EXECUTION_RETRIED",
                "POLICE_EXECUTION",
                executionId,
                auditHash,
                normalizedPartnerSystem + "#attempt=" + attempts
        );
        return snapshot(updated);
    }

    private Map<String, Object> snapshot(TraceableExecutionState state) {
        LinkedHashMap<String, Object> retryPolicy = new LinkedHashMap<>();
        retryPolicy.put("attemptsUsed", state.attempts());
        retryPolicy.put("retryLimit", state.retryLimit());
        putIfNotNull(retryPolicy, "nextRetryAt", state.nextRetryAt() == null ? null : state.nextRetryAt().toString());
        retryPolicy.put("strategy", "EXPONENCIAL_COM_TETO_E_JANELA_SOBERANA");
        retryPolicy.put("manualOverrideAllowed", Boolean.TRUE);
        LinkedHashMap<String, Object> queues = new LinkedHashMap<>();
        queues.put("confirmationQueue", state.confirmationQueue());
        queues.put("errorQueue", state.errorQueue());
        queues.put("reconciliationQueue", state.reconciliationQueue());
        LinkedHashMap<String, Object> integrity = new LinkedHashMap<>();
        integrity.put("routeHash", state.routeHash());
        integrity.put("auditHash", state.auditHash());
        integrity.put("idempotencyKey", state.idempotencyKey());
        integrity.put("hashModel", "SHA256_ROUTE_AND_STATUS");
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "POLICE_TRACEABLE_EXECUTION_STATUS");
        out.put("executionId", state.executionId());
        out.put("actorLane", state.actorLane());
        out.put("operationCode", state.operationCode());
        putIfNotNull(out, "inqueritoId", state.inqueritoId());
        putIfNotNull(out, "processoId", state.processoId());
        out.put("partnerSystem", state.partnerSystem());
        out.put("transactionFamily", state.transactionFamily());
        out.put("priorityLane", state.priorityLane());
        out.put("status", state.currentStatus());
        out.put("nativeFirstStrict", state.nativeFirstStrict());
        out.put("createdAt", state.createdAt().toString());
        out.put("updatedAt", state.updatedAt().toString());
        out.put("partnerFallbackOrder", state.partnerFallbackOrder());
        out.put("nextActions", state.nextActions());
        out.put("queues", immutableMap(queues));
        out.put("retryPolicy", immutableMap(retryPolicy));
        out.put("integrity", immutableMap(integrity));
        out.put("executionPlan", state.executionPlan());
        out.put("lastPartnerSignal", state.lastPartnerSignal());
        out.put("statusTrail", state.statusTrail());
        out.put("operationalLedger", operationalLedgerBlueprint(state.actorLane()));
        return immutableMap(out);
    }

    private Map<String, Object> operationalLedgerBlueprint(String actorLane) {
        LinkedHashMap<String, Object> confirmations = new LinkedHashMap<>();
        confirmations.put("confirmationQueuePrefix", queueName(actorLane, "PARTNER", "CONFIRMATION"));
        confirmations.put("errorQueuePrefix", queueName(actorLane, "PARTNER", "ERROR"));
        confirmations.put("reconciliationQueuePrefix", queueName(actorLane, "PARTNER", "RECONCILIATION"));
        confirmations.put("statusLifecycle", List.of(
                "AGUARDANDO_CONFIRMACAO_EXTERNA",
                "CONFIRMADO_PELO_PARCEIRO",
                "EM_RECONCILIACAO",
                "REENTRADA_ENFILEIRADA",
                "ERRO_TRANSACIONAL",
                "ERRO_RECONCILIADO",
                "FECHADO_LOCAL"
        ));
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "POLICE_TRACEABLE_OPERATIONAL_LEDGER");
        out.put("actorLane", actorLane);
        out.put("nativeFirst", Boolean.TRUE);
        out.put("operationalLedger", List.of(
                "execution_id_estavel",
                "hash_operacional_por_rota_e_status",
                "status_trail_imutavel_em_memoria_operacional",
                "filas_de_confirmacao_erro_e_reconciliacao",
                "retentativa_com_backoff_e_teto",
                "reconciliacao_com_parceiro_e_snapshot"
        ));
        out.put("confirmationFlow", Map.copyOf(confirmations));
        out.put("reconciliationModes", List.of(
                "HASH_ROUTE_VS_PARTNER",
                "SNAPSHOT_LOCAL_VS_PARCEIRO",
                "CONFIRMACAO_PROTOCOLO_EXTERNO"
        ));
        out.put("retryPolicy", Map.of(
                "strategy", "EXPONENCIAL_COM_TETO_E_APROVACAO_OPERACIONAL",
                "defaultLimit", DEFAULT_RETRY_LIMIT,
                "backoffMinutes", List.of(2, 5, 15, 30, 60, 180, 360),
                "deadLetterQueue", queueName(actorLane, "PARTNER", "DLQ")
        ));
        out.put("auditBackbone", List.of(
                "audit_ledger_append_only",
                "idempotency_key_por_execucao",
                "route_hash_sha256",
                "audit_hash_sha256"
        ));
        return immutableMap(out);
    }

    private Map<String, Object> notFound(String executionId) {
        return Map.of(
                "mode", "POLICE_TRACEABLE_EXECUTION_STATUS",
                "found", false,
                "executionId", executionId,
                "status", "EXECUCAO_NAO_LOCALIZADA"
        );
    }

    private Map<String, Object> accessMismatch(String executionId, String requestedLane, String storedLane) {
        return Map.of(
                "mode", "POLICE_TRACEABLE_EXECUTION_STATUS",
                "found", false,
                "executionId", executionId,
                "status", "EXECUCAO_FORA_DA_LANE_AUTORIZADA",
                "requestedLane", requestedLane,
                "storedLane", storedLane
        );
    }

    private void cleanupIfRequired(Instant now, boolean force) {
        long current = System.nanoTime();
        long scheduled = nextCleanupAtNanos.get();
        if (!force && current < scheduled) {
            return;
        }
        if (!force && !nextCleanupAtNanos.compareAndSet(scheduled, current + CLEANUP_INTERVAL_NANOS)) {
            return;
        }
        if (force) {
            nextCleanupAtNanos.set(current + CLEANUP_INTERVAL_NANOS);
        }
        pruneExpired(now);
        if (states.size() > MAX_ACTIVE_EXECUTIONS) {
            trimOverflow();
        }
    }

    private void pruneExpired(Instant now) {
        states.entrySet().removeIf(entry -> isExpired(entry.getValue(), now));
    }

    private void trimOverflow() {
        int overflow = states.size() - MAX_ACTIVE_EXECUTIONS;
        if (overflow <= 0) {
            return;
        }
        List<TraceableExecutionState> candidates = states.values().stream()
                .sorted(Comparator.comparingInt((TraceableExecutionState state) -> terminalRank(state.currentStatus()))
                        .thenComparing(TraceableExecutionState::updatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .limit(overflow)
                .toList();
        for (TraceableExecutionState candidate : candidates) {
            if (candidate != null) {
                states.remove(candidate.executionId(), candidate);
            }
        }
    }

    private boolean isExpired(TraceableExecutionState state, Instant now) {
        if (state == null || state.updatedAt() == null) {
            return true;
        }
        Duration retention = isTerminal(state.currentStatus()) ? TERMINAL_RETENTION : ACTIVE_RETENTION;
        return state.updatedAt().plus(retention).isBefore(now);
    }

    private static int terminalRank(String status) {
        return isTerminal(status) ? 0 : 1;
    }

    private static boolean isTerminal(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return switch (status) {
            case "CONFIRMADO_PELO_PARCEIRO", "FECHADO_LOCAL", "ERRO_RECONCILIADO", "ERRO_TRANSACIONAL" -> true;
            default -> false;
        };
    }

    private static String resolvePartnerStatus(String partnerStatus, boolean confirmDelivery, boolean reconciliarSnapshot) {
        if (partnerStatus.contains("PROTOCOLO") || partnerStatus.contains("ACEITO") || partnerStatus.contains("CONFIRM")) {
            return reconciliarSnapshot ? "EM_RECONCILIACAO" : "CONFIRMADO_PELO_PARCEIRO";
        }
        if (partnerStatus.contains("REJEIT") || partnerStatus.contains("ERRO") || partnerStatus.contains("FALHA")) {
            return "ERRO_RECONCILIADO";
        }
        if (confirmDelivery) {
            return reconciliarSnapshot ? "EM_RECONCILIACAO" : "CONFIRMADO_PELO_PARCEIRO";
        }
        return "AGUARDANDO_CONFIRMACAO_EXTERNA";
    }

    private static Instant computeNextRetryAt(int attempts) {
        long[] backoffMinutes = {2, 5, 15, 30, 60, 180, 360};
        int index = Math.max(0, Math.min(backoffMinutes.length - 1, Math.max(0, attempts - 1)));
        return Instant.now().plus(backoffMinutes[index], ChronoUnit.MINUTES);
    }

    private static String queueName(String actorLane, String partnerSystem, String suffix) {
        String safePartner = normalize(partnerSystem, "PARTNER").toLowerCase(Locale.ROOT);
        return "queue." + actorLane.toLowerCase(Locale.ROOT) + "." + safePartner + "." + suffix.toLowerCase(Locale.ROOT);
    }

    private static Map<String, Object> statusEvent(String eventCode,
                                                   String status,
                                                   String description,
                                                   String routeHash,
                                                   Instant occurredAt,
                                                   int attempts) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("eventCode", eventCode);
        out.put("status", status);
        out.put("description", description);
        out.put("attempts", attempts);
        out.put("occurredAt", occurredAt.toString());
        out.put("eventHash", Hashes.sha256Hex(String.join("|", eventCode, status, description, routeHash, occurredAt.toString(), String.valueOf(attempts))));
        return Collections.unmodifiableMap(out);
    }

    private static String computeAuditHash(String executionId,
                                           String status,
                                           int attempts,
                                           List<Map<String, Object>> trail,
                                           Map<String, Object> executionPlan,
                                           String partnerSystem) {
        return Hashes.sha256Hex(String.join("|",
                executionId,
                status,
                String.valueOf(attempts),
                partnerSystem,
                String.valueOf(trail),
                String.valueOf(executionPlan)));
    }

    private static String actorLane(TipoUsuario tipoUsuario) {
        return tipoUsuario == TipoUsuario.DELEGADO_POLICIA_FEDERAL ? "POLICIA_FEDERAL" : "POLICIA_CIVIL";
    }

    private static String normalize(String value, String fallback) {
        String safe = value == null || value.isBlank() ? fallback : value.trim();
        return safe.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static List<String> immutableStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            out.add(value.trim());
        }
        return List.copyOf(out);
    }

    private static void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private static Map<String, Object> immutableMap(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> safe = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (value != null) {
                safe.put(key, value);
            }
        });
        return Map.copyOf(safe);
    }

    private record TraceableExecutionState(
            String executionId,
            String actorLane,
            String operationCode,
            Long inqueritoId,
            Long processoId,
            String partnerSystem,
            String transactionFamily,
            boolean nativeFirstStrict,
            String priorityLane,
            String currentStatus,
            int attempts,
            int retryLimit,
            Instant createdAt,
            Instant updatedAt,
            Instant nextRetryAt,
            String confirmationQueue,
            String errorQueue,
            String reconciliationQueue,
            String idempotencyKey,
            String routeHash,
            String auditHash,
            List<String> partnerFallbackOrder,
            List<String> nextActions,
            Map<String, Object> executionPlan,
            Map<String, Object> lastPartnerSignal,
            List<Map<String, Object>> statusTrail
    ) {
    }
}
