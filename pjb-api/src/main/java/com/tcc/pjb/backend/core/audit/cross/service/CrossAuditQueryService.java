package com.tcc.pjb.backend.core.audit.cross.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.audit.cross.persistence.entity.AuditCorrelationIndexEntity;
import com.tcc.pjb.backend.core.audit.cross.persistence.entity.CrossAuditLinkEntity;
import com.tcc.pjb.backend.core.audit.cross.persistence.repo.AuditCorrelationIndexRepository;
import com.tcc.pjb.backend.core.audit.cross.persistence.repo.CrossAuditLinkRepository;

@Service
public class CrossAuditQueryService {

    private final AuditCorrelationIndexRepository indexRepo;
    private final CrossAuditLinkRepository linkRepo;

    public CrossAuditQueryService(AuditCorrelationIndexRepository indexRepo, CrossAuditLinkRepository linkRepo) {
        this.indexRepo = indexRepo;
        this.linkRepo = linkRepo;
    }

    public List<AuditCorrelationIndexEntity> byKey(String correlationKey, int limit) {
        if (correlationKey == null || correlationKey.isBlank()) return List.of();
        
        return indexRepo.findTop50ByCorrelationKeyOrderByCreatedAtDesc(correlationKey.trim());
    }

    public List<CrossAuditLinkEntity> linksByKey(String correlationKey, int limit) {
        if (correlationKey == null || correlationKey.isBlank()) return List.of();
        return linkRepo.findTop50ByCorrelationKeyOrderByCreatedAtDesc(correlationKey.trim());
    }

    public List<CrossAuditLinkEntity> forResource(String resourceType, String resourceId, int limit) {
        if (resourceType == null || resourceType.isBlank() || resourceId == null || resourceId.isBlank()) return List.of();
        List<CrossAuditLinkEntity> out = new ArrayList<>();
        out.addAll(linkRepo.findTop50ByLeftResourceTypeAndLeftResourceIdOrderByCreatedAtDesc(resourceType.trim(), resourceId.trim()));
        out.addAll(linkRepo.findTop50ByRightResourceTypeAndRightResourceIdOrderByCreatedAtDesc(resourceType.trim(), resourceId.trim()));
        return out;
    }
}
