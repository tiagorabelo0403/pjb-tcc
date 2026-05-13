package com.tcc.pjb.backend.core.audit.cross.api;

import java.time.Instant;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.core.audit.cross.persistence.entity.AuditCorrelationIndexEntity;
import com.tcc.pjb.backend.core.audit.cross.persistence.entity.CrossAuditLinkEntity;
import com.tcc.pjb.backend.core.audit.cross.service.CrossAuditQueryService;

@RestController
@RequestMapping("/api/internal/audit/v1/cross")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR')")
public class CrossAuditInternalController {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 50;

    private final CrossAuditQueryService service;

    public CrossAuditInternalController(CrossAuditQueryService service) {
        this.service = service;
    }

    @GetMapping("/by-key/{correlationKey}")
    public List<CorrelationView> byKey(@PathVariable String correlationKey,
                                       @RequestParam(name = "limit", defaultValue = "50") int limit) {
        int sanitizedLimit = sanitizeLimit(limit);
        return service.byKey(correlationKey, sanitizedLimit).stream()
                .limit(sanitizedLimit)
                .map(CrossAuditInternalController::toView)
                .toList();
    }

    @GetMapping("/for/{resourceType}/{resourceId}")
    public List<LinkView> forResource(@PathVariable String resourceType,
                                      @PathVariable String resourceId,
                                      @RequestParam(name = "limit", defaultValue = "50") int limit) {
        int sanitizedLimit = sanitizeLimit(limit);
        return service.forResource(resourceType, resourceId, sanitizedLimit).stream()
                .limit(sanitizedLimit)
                .map(CrossAuditInternalController::toView)
                .toList();
    }

    private int sanitizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static CorrelationView toView(AuditCorrelationIndexEntity entity) {
        return new CorrelationView(
                entity.getId(),
                entity.getCorrelationKey(),
                entity.getResourceType(),
                entity.getResourceId(),
                entity.getCreatedAt()
        );
    }

    private static LinkView toView(CrossAuditLinkEntity entity) {
        return new LinkView(
                entity.getId(),
                entity.getCorrelationKey(),
                entity.getLeftResourceType(),
                entity.getLeftResourceId(),
                entity.getRightResourceType(),
                entity.getRightResourceId(),
                entity.getCreatedAt()
        );
    }

    public record CorrelationView(Long id,
                                  String correlationKey,
                                  String resourceType,
                                  String resourceId,
                                  Instant createdAt) {
    }

    public record LinkView(Long id,
                           String correlationKey,
                           String leftResourceType,
                           String leftResourceId,
                           String rightResourceType,
                           String rightResourceId,
                           Instant createdAt) {
    }
}
