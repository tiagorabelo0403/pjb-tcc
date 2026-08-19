package com.tcc.pjb.backend.core.audit.ledger;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.util.Hashes;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuditLedgerService {

    private static final int MAX_ENTRIES = 10_000;

    private final ConcurrentLinkedDeque<AuditLedgerEntry> entries = new ConcurrentLinkedDeque<>();
    private final AtomicInteger entryCount = new AtomicInteger();
    private final AuditLedgerRepository auditLedgerRepository;
    private final CurrentUserService currentUserService;
    private final Counter persistFailureCounter;

    public AuditLedgerService(AuditLedgerRepository auditLedgerRepository, CurrentUserService currentUserService, MeterRegistry registry) {
        this.auditLedgerRepository = Objects.requireNonNull(auditLedgerRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.persistFailureCounter = Counter.builder("pjb.audit_ledger.persist_failures")
                .description("Entradas de auditoria que falharam ao persistir (engolidas por appendSafely, nunca lançadas ao chamador)")
                .register(Objects.requireNonNull(registry));
    }

    private static final int PAYLOAD_HASH_MAX_LENGTH = 64;

    /**
     * REQUIRES_NEW em todo ponto de entrada publico (aqui e nos 4 overloads de {@code
     * appendSafely} abaixo — nao apenas neste metodo, porque appendSafely chama append via
     * self-invocation, que nao passa pelo proxy do Spring e ignoraria uma anotacao so aqui):
     * a escrita do log de auditoria precisa ser isolada da transacao do chamador. Sem isso, um
     * chamador com {@code @Transactional(readOnly = true)} (ex.: EquipeSwitchInterceptor ->
     * OfficeWorkspaceModeService.current() -> buildView()) tem o INSERT rejeitado pelo Postgres
     * ("cannot execute INSERT in a read-only transaction"); persistSafely engole a excecao, mas a
     * transacao ambiente ja fica marcada rollback-only e a chamada inteira falha com
     * UnexpectedRollbackException no commit — mesmo em codigo que nunca tocou auditoria
     * diretamente. Bug real, achado com Postgres real nesta investigacao.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLedgerEntry append(String eventCode,
                                   String resourceType,
                                   String resourceId,
                                   String payloadHash,
                                   String description) {
        AuditLedgerEntry entry = new AuditLedgerEntry(
                eventCode,
                resourceType,
                resourceId,
                safePayloadHash(eventCode, resourceType, resourceId, payloadHash, description),
                description,
                Instant.now()
        );
        entries.addLast(entry);
        int size = entryCount.incrementAndGet();
        trimOverflow(size);
        persistSafely(entry);
        return entry;
    }

    private String safePayloadHash(String eventCode, String resourceType, String resourceId, String payloadHash, String description) {
        if (payloadHash == null || payloadHash.isBlank()) {
            return Hashes.sha256Hex(String.join("#", String.valueOf(eventCode), String.valueOf(resourceType), String.valueOf(resourceId), String.valueOf(description), String.valueOf(Instant.now())));
        }
        if (payloadHash.length() > PAYLOAD_HASH_MAX_LENGTH) {
            return Hashes.sha256Hex(payloadHash);
        }
        return payloadHash;
    }

    private void persistSafely(AuditLedgerEntry entry) {
        try {
            var usuario = currentUserService.getOrNull();
            if (usuario != null) {
                entry.setActorUserId(usuario.getId());
            }
            auditLedgerRepository.save(entry);
        } catch (Exception e) {
            log.warn("Falha ao persistir entrada de auditoria (não bloqueante): action={} erro={}", entry.getAction(), e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLedgerEntry appendSafely(String eventCode, String resourceType, String resourceId, String payloadHash) {
        return append(eventCode, resourceType, resourceId, payloadHash, "");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLedgerEntry appendSafely(String eventCode, String description) {
        return append(eventCode, "AUDIT", "N/A", null, description);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLedgerEntry appendSafely(String eventCode, String resourceType, String resourceId) {
        return append(eventCode, resourceType, resourceId, null, "");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLedgerEntry appendSafely(String eventCode, String resourceType, String resourceId, String payloadHash, String description) {
        return append(eventCode, resourceType, resourceId, payloadHash, description);
    }

    public List<AuditLedgerEntry> entries() {
        return List.copyOf(new ArrayList<>(entries));
    }

    private void trimOverflow(int size) {
        while (size > MAX_ENTRIES) {
            AuditLedgerEntry removed = entries.pollFirst();
            if (removed == null) {
                entryCount.compareAndSet(size, 0);
                return;
            }
            size = entryCount.decrementAndGet();
        }
    }
}
