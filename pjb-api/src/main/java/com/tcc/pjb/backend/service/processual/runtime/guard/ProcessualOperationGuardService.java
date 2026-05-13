package com.tcc.pjb.backend.service.processual.runtime.guard;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.idempotency.ActionIdempotencyService;
import com.tcc.pjb.backend.core.idempotency.IdempotencyBeginResult;
import com.tcc.pjb.backend.core.idempotency.IdempotencyDecision;
import com.tcc.pjb.backend.core.resilience.LocalCircuitBreaker;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.dto.processual.runtime.guard.ProcessualOperationGuardRequest;
import com.tcc.pjb.backend.model.dto.processual.runtime.homologation.ProcessualHomologationGateStatusResponse;
import com.tcc.pjb.backend.model.dto.processual.runtime.guard.ProcessualOperationGuardResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.service.processual.runtime.homologation.GateTravamentoHomologacaoService;

@Service
public class ProcessualOperationGuardService {

    private final ProcessoRepository processoRepository;
    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final ActionIdempotencyService actionIdempotencyService;
    private final OutboxPublisher outboxPublisher;
    private static final int MAX_BREAKERS = 20_000;
    private static final long BREAKER_IDLE_MILLIS = Duration.ofMinutes(30).toMillis();

    private final ObjectMapper objectMapper;
    private final GateTravamentoHomologacaoService gateTravamentoHomologacaoService;
    private final Clock clock;
    private final ConcurrentMap<String, BreakerSlot> breakers = new ConcurrentHashMap<>();
    private final AtomicLong breakerPruneSequence = new AtomicLong();

    public ProcessualOperationGuardService(ProcessoRepository processoRepository,
                                           CurrentUserService currentUserService,
                                           PjbAuthorizationService authorizationService,
                                           ActionIdempotencyService actionIdempotencyService,
                                           OutboxPublisher outboxPublisher,
                                           ObjectMapper objectMapper,
                                           GateTravamentoHomologacaoService gateTravamentoHomologacaoService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.actionIdempotencyService = Objects.requireNonNull(actionIdempotencyService);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.gateTravamentoHomologacaoService = Objects.requireNonNull(gateTravamentoHomologacaoService);
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public ProcessualHomologationGateStatusResponse avaliarHomologacao(Long processoId, String operationCode) {
        Processo processo = loadAndAuthorize(processoId);
        return gateTravamentoHomologacaoService.avaliar(processo.getId(), operationCode);
    }

    @Transactional
    public ProcessualOperationGuardResponse guard(ProcessualOperationGuardRequest request) {
        Objects.requireNonNull(request);
        Processo processo = loadAndAuthorize(request.processoId());
        Usuario usuario = currentUserService.getRequired();
        String operationCode = normalizeOperationCode(request.operationCode());
        String scope = buildScope(operationCode, processo);
        String requestHash = resolveRequestHash(request, processo, usuario, scope);
        String idempotencyKey = resolveIdempotencyKey(request.idempotencyKey(), scope, requestHash);
        var homologationGateAssessment = processo == null ? null : gateTravamentoHomologacaoService.avaliar(processo.getId(), operationCode);
        if (homologationGateAssessment != null && homologationGateAssessment.blocked()) {
            return new ProcessualOperationGuardResponse(
                    processo.getId(),
                    operationCode,
                    scope,
                    idempotencyKey,
                    "GUARD_BLOCKED",
                    "BLOCKED_BY_GATE",
                    false,
                    "CLOSED",
                    "PROCESSUAL_OPERATION_GUARD",
                    String.valueOf(processo.getId()),
                    null,
                    null,
                    homologationGateAssessment.details().stream().map(detail -> detail.descricao()).toList(),
                    metadata(request, processo, usuario, requestHash, false, homologationGateAssessment),
                    Instant.now()
            );
        }
        LocalCircuitBreaker breaker = resolveBreaker(scope);
        if (!breaker.tryAcquire()) {
            return new ProcessualOperationGuardResponse(
                    processo != null ? processo.getId() : null,
                    operationCode,
                    scope,
                    idempotencyKey,
                    "CIRCUIT_OPEN",
                    "BLOCKED",
                    false,
                    breaker.state().name(),
                    "PROCESSUAL_OPERATION_GUARD",
                    processo != null && processo.getId() != null ? String.valueOf(processo.getId()) : null,
                    null,
                    null,
                    List.of("Operação temporariamente bloqueada pela proteção de resiliência local."),
                    metadata(request, processo, usuario, requestHash, false, homologationGateAssessment),
                    Instant.now()
            );
        }
        IdempotencyBeginResult begin = actionIdempotencyService.begin(scope, idempotencyKey, requestHash, Duration.ofSeconds(45));
        if (begin.decision() != IdempotencyDecision.NEW) {
            breaker.recordSuccess();
            boolean accepted = begin.decision() == IdempotencyDecision.REPLAY;
            List<String> warnings = begin.decision() == IdempotencyDecision.IN_PROGRESS
                    ? List.of("Operação já está em andamento para a mesma chave de idempotência.")
                    : List.of("Operação reaproveitou resposta previamente consolidada.");
            return new ProcessualOperationGuardResponse(
                    processo != null ? processo.getId() : null,
                    operationCode,
                    scope,
                    idempotencyKey,
                    begin.decision().name(),
                    begin.status().name(),
                    accepted,
                    breaker.state().name(),
                    begin.resourceType(),
                    begin.resourceId(),
                    begin.responseJson(),
                    null,
                    warnings,
                    metadata(request, processo, usuario, requestHash, accepted, homologationGateAssessment),
                    Instant.now()
            );
        }
        try {
            String outboxEventId = null;
            if (Boolean.TRUE.equals(request.emitOutbox())) {
                UUID eventId = outboxPublisher.enqueueTracked(
                        "processual.guard",
                        "PROCESSUAL_OPERATION_GUARD",
                        PayloadMaps.ofEntries(
                                "processoId", processo != null ? processo.getId() : null,
                                "numeroProcesso", processo != null ? processo.getNumeroProcesso() : null,
                                "operationCode", operationCode,
                                "usuarioId", usuario.getId(),
                                "usuarioPerfil", usuario.getTipoUsuario() != null ? usuario.getTipoUsuario().name() : null,
                                "metadata", request.metadata(),
                                "guardedAt", Instant.now().toString()
                        ),
                        PayloadMaps.ofEntries(
                                "scope", scope,
                                "idempotencyKey", idempotencyKey,
                                "requestHash", requestHash
                        ),
                        "processual:guard:" + scope + ":" + idempotencyKey,
                        firstNonBlank(request.aggregateType(), "PROCESSO"),
                        firstNonBlank(request.aggregateId(), processo != null && processo.getId() != null ? String.valueOf(processo.getId()) : null)
                );
                outboxEventId = eventId.toString();
            }
            ProcessualOperationGuardResponse response = new ProcessualOperationGuardResponse(
                    processo != null ? processo.getId() : null,
                    operationCode,
                    scope,
                    idempotencyKey,
                    IdempotencyDecision.NEW.name(),
                    "COMPLETED",
                    true,
                    breaker.state().name(),
                    "PROCESSUAL_OPERATION_GUARD",
                    processo != null && processo.getId() != null ? String.valueOf(processo.getId()) : null,
                    null,
                    outboxEventId,
                    List.of(),
                    metadata(request, processo, usuario, requestHash, true, homologationGateAssessment),
                    Instant.now()
            );
            String responseJson = toJson(PayloadMaps.ofEntries(
                    "processoId", response.processoId(),
                    "operationCode", response.operationCode(),
                    "idempotencyScope", response.idempotencyScope(),
                    "idempotencyKey", response.idempotencyKey(),
                    "accepted", response.accepted(),
                    "outboxEventId", response.outboxEventId(),
                    "processedAt", response.processedAt().toString()
            ));
            actionIdempotencyService.complete(
                    scope,
                    idempotencyKey,
                    Hashes.sha256Hex(responseJson),
                    response.resourceType(),
                    response.resourceId(),
                    responseJson
            );
            breaker.recordSuccess();
            return new ProcessualOperationGuardResponse(
                    response.processoId(),
                    response.operationCode(),
                    response.idempotencyScope(),
                    response.idempotencyKey(),
                    response.idempotencyDecision(),
                    response.idempotencyStatus(),
                    response.accepted(),
                    breaker.state().name(),
                    response.resourceType(),
                    response.resourceId(),
                    responseJson,
                    response.outboxEventId(),
                    response.warnings(),
                    response.metadata(),
                    response.processedAt()
            );
        } catch (RuntimeException ex) {
            breaker.recordFailure();
            actionIdempotencyService.fail(scope, idempotencyKey, Hashes.sha256Hex(ex.getClass().getName() + ':' + safe(ex.getMessage())), safe(ex.getMessage()));
            throw ex;
        }
    }


    private LocalCircuitBreaker resolveBreaker(String scope) {
        long now = clock.millis();
        pruneBreakersIfRequired(now, false);
        BreakerSlot slot = breakers.compute(scope, (key, current) -> {
            if (current == null) {
                return new BreakerSlot(new LocalCircuitBreaker(clock, 3, 30_000L), now);
            }
            current.touch(now);
            return current;
        });
        if (slot == null) {
            throw new IllegalStateException("processual breaker unavailable");
        }
        pruneBreakersIfRequired(now, breakers.size() > MAX_BREAKERS);
        return slot.breaker();
    }

    private void pruneBreakersIfRequired(long now, boolean force) {
        long seq = breakerPruneSequence.incrementAndGet();
        if (!force && (seq & 255L) != 0L) {
            return;
        }
        breakers.entrySet().removeIf(entry -> entry == null || entry.getValue() == null || entry.getValue().idleAt(now, BREAKER_IDLE_MILLIS));
        int overflow = breakers.size() - MAX_BREAKERS;
        if (overflow <= 0) {
            return;
        }
        List<Map.Entry<String, BreakerSlot>> ordered = new java.util.ArrayList<>(breakers.entrySet());
        ordered.sort(java.util.Comparator.comparingLong(item -> item.getValue() == null ? Long.MIN_VALUE : item.getValue().lastTouchedAtEpochMilli()));
        for (Map.Entry<String, BreakerSlot> entry : ordered) {
            if (overflow <= 0) {
                break;
            }
            if (entry == null || entry.getKey() == null) {
                continue;
            }
            if (breakers.remove(entry.getKey(), entry.getValue())) {
                overflow--;
            }
        }
    }

    private Processo loadAndAuthorize(Long processoId) {
        if (processoId == null) {
            return null;
        }
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        authorizationService.requireReadProcesso(processo);
        return processo;
    }

    private String buildScope(String operationCode, Processo processo) {
        return processo != null && processo.getId() != null
                ? "PROCESSUAL:" + operationCode + ":" + processo.getId()
                : "PROCESSUAL:" + operationCode + ":GLOBAL";
    }

    private String resolveRequestHash(ProcessualOperationGuardRequest request,
                                      Processo processo,
                                      Usuario usuario,
                                      String scope) {
        if (request.payloadHash() != null && !request.payloadHash().isBlank()) {
            return request.payloadHash().trim();
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("scope", scope);
        payload.put("processoId", processo != null ? processo.getId() : null);
        payload.put("numeroProcesso", processo != null ? processo.getNumeroProcesso() : null);
        payload.put("operationCode", normalizeOperationCode(request.operationCode()));
        payload.put("usuarioId", usuario.getId());
        payload.put("usuarioPerfil", usuario.getTipoUsuario() != null ? usuario.getTipoUsuario().name() : null);
        payload.put("metadata", request.metadata());
        return Hashes.sha256Hex(toJson(payload));
    }

    private String resolveIdempotencyKey(String rawKey, String scope, String requestHash) {
        if (rawKey != null && !rawKey.isBlank()) {
            return rawKey.trim();
        }
        return UUID.nameUUIDFromBytes((scope + '|' + requestHash).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String normalizeOperationCode(String value) {
        String normalized = firstNonBlank(value, "PROCESSUAL_OPERATION");
        return normalized.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
    }

    private Map<String, Object> metadata(ProcessualOperationGuardRequest request,
                                         Processo processo,
                                         Usuario usuario,
                                         String requestHash,
                                         boolean accepted,
                                         ProcessualHomologationGateStatusResponse homologationGateAssessment) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("processoNumero", processo != null ? processo.getNumeroProcesso() : null);
        out.put("usuarioId", usuario.getId());
        out.put("usuarioPerfil", usuario.getTipoUsuario() != null ? usuario.getTipoUsuario().name() : null);
        out.put("emitOutbox", request.emitOutbox());
        out.put("requestHash", requestHash);
        out.put("accepted", accepted);
        if (homologationGateAssessment != null) {
            out.put("homologationGateAssessment", homologationGateAssessment);
        }
        if (request.metadata() != null && !request.metadata().isEmpty()) {
            out.put("requestMetadata", request.metadata());
        }
        out.values().removeIf(Objects::isNull);
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("processual operation guard json", ex);
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class BreakerSlot {
        private final LocalCircuitBreaker breaker;
        private final AtomicLong lastTouchedAtEpochMilli;

        private BreakerSlot(LocalCircuitBreaker breaker, long touchedAt) {
            this.breaker = breaker;
            this.lastTouchedAtEpochMilli = new AtomicLong(touchedAt);
        }

        private LocalCircuitBreaker breaker() {
            return breaker;
        }

        private long lastTouchedAtEpochMilli() {
            return lastTouchedAtEpochMilli.get();
        }

        private void touch(long now) {
            lastTouchedAtEpochMilli.set(now);
        }

        private boolean idleAt(long now, long maxIdleMillis) {
            return now - lastTouchedAtEpochMilli() >= maxIdleMillis;
        }
    }
}
