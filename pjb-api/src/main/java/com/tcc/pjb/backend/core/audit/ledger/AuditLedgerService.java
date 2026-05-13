package com.tcc.pjb.backend.core.audit.ledger;

import com.tcc.pjb.backend.core.util.Hashes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class AuditLedgerService {

    private static final int MAX_ENTRIES = 10_000;

    private final ConcurrentLinkedDeque<AuditLedgerEntry> entries = new ConcurrentLinkedDeque<>();
    private final AtomicInteger entryCount = new AtomicInteger();

    public AuditLedgerEntry append(String eventCode,
                                   String resourceType,
                                   String resourceId,
                                   String payloadHash,
                                   String description) {
        AuditLedgerEntry entry = new AuditLedgerEntry(
                eventCode,
                resourceType,
                resourceId,
                payloadHash == null || payloadHash.isBlank() ? Hashes.sha256Hex(String.join("#", String.valueOf(eventCode), String.valueOf(resourceType), String.valueOf(resourceId), String.valueOf(description), String.valueOf(Instant.now()))) : payloadHash,
                description,
                Instant.now()
        );
        entries.addLast(entry);
        int size = entryCount.incrementAndGet();
        trimOverflow(size);
        return entry;
    }

    public AuditLedgerEntry appendSafely(String eventCode, String resourceType, String resourceId, String payloadHash) {
        return append(eventCode, resourceType, resourceId, payloadHash, "");
    }

    public AuditLedgerEntry appendSafely(String eventCode, String description) {
        return append(eventCode, "AUDIT", "N/A", null, description);
    }

    public AuditLedgerEntry appendSafely(String eventCode, String resourceType, String resourceId) {
        return append(eventCode, resourceType, resourceId, null, "");
    }

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
