package com.tcc.pjb.backend.core.audit.cross.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.cross.contract.CrossAuditSignal;
import com.tcc.pjb.backend.core.audit.cross.persistence.entity.AuditCorrelationIndexEntity;
import com.tcc.pjb.backend.core.audit.cross.persistence.entity.CrossAuditLinkEntity;
import com.tcc.pjb.backend.core.audit.cross.persistence.repo.AuditCorrelationIndexRepository;
import com.tcc.pjb.backend.core.audit.cross.persistence.repo.CrossAuditLinkRepository;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Component
public class CrossAuditSignalListener {

    private final AuditCorrelationIndexRepository indexRepo;
    private final CrossAuditLinkRepository linkRepo;
    private final AuditLedgerService ledger;

    public CrossAuditSignalListener(AuditCorrelationIndexRepository indexRepo,
                                   CrossAuditLinkRepository linkRepo,
                                   AuditLedgerService ledger) {
        this.indexRepo = indexRepo;
        this.linkRepo = linkRepo;
        this.ledger = ledger;
    }

    @PjbTransactionalBudget(operation = "audit.cross-signal-listener.on", maxMillis = 3000)
    @EventListener
    @Transactional
    public void on(CrossAuditSignal signal) {
        if (signal == null) return;

        String key = trim(signal.correlationKey());
        String rt = trim(signal.resourceType());
        String rid = trim(signal.resourceId());
        if (key.isEmpty() || rt.isEmpty() || rid.isEmpty()) return;

        
        AuditCorrelationIndexEntity idx = new AuditCorrelationIndexEntity();
        idx.setCorrelationKey(key);
        idx.setResourceType(rt);
        idx.setResourceId(rid);
        idx.setCreatedAt(Instant.now());
        try {
            indexRepo.save(idx);
        } catch (Exception ignored) {
            
        }

        
        List<AuditCorrelationIndexEntity> existing = indexRepo.findByCorrelationKey(key);
        List<CrossAuditLinkEntity> links = new ArrayList<>();
        Instant now = Instant.now();
        for (AuditCorrelationIndexEntity e : existing) {
            if (e == null) continue;
            if (Objects.equals(e.getResourceType(), rt) && Objects.equals(e.getResourceId(), rid)) continue;

            CrossAuditLinkEntity link = new CrossAuditLinkEntity();
            link.setCorrelationKey(key);
            link.setLeftResourceType(rt);
            link.setLeftResourceId(rid);
            link.setRightResourceType(e.getResourceType());
            link.setRightResourceId(e.getResourceId());
            link.setCreatedAt(now);
            links.add(link);
        }

        if (!links.isEmpty()) {
            try {
                linkRepo.saveAll(links);
            } catch (Exception ex) {
                
                for (CrossAuditLinkEntity link : links) {
                    try {
                        linkRepo.save(link);
                    } catch (Exception ignored) {
                        
                    }
                }
            }
        }

        
        String payloadHash = signal.payloadHash();
        ledger.appendSafely("CROSS_AUDIT_SIGNAL", "CORRELATION_KEY", key, payloadHash);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
