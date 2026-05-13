package com.tcc.pjb.backend.service.admin.surface;

import com.tcc.pjb.backend.model.dto.admin.governance.AdminProceduralSnapshotResponse;
import com.tcc.pjb.backend.service.procedural.ProceduralArchitectureSanityService;
import com.tcc.pjb.backend.service.procedural.ProceduralLegacyBoundaryAuditService;
import com.tcc.pjb.backend.service.procedural.ProceduralStartupSanityGuard;
import org.springframework.stereotype.Service;

@Service
public class AdminProceduralGovernanceFacadeService {

    private final ProceduralArchitectureSanityService proceduralArchitectureSanityService;
    private final ProceduralLegacyBoundaryAuditService proceduralLegacyBoundaryAuditService;
    private final ProceduralStartupSanityGuard proceduralStartupSanityGuard;

    public AdminProceduralGovernanceFacadeService(ProceduralArchitectureSanityService proceduralArchitectureSanityService,
                                                  ProceduralLegacyBoundaryAuditService proceduralLegacyBoundaryAuditService,
                                                  ProceduralStartupSanityGuard proceduralStartupSanityGuard) {
        this.proceduralArchitectureSanityService = proceduralArchitectureSanityService;
        this.proceduralLegacyBoundaryAuditService = proceduralLegacyBoundaryAuditService;
        this.proceduralStartupSanityGuard = proceduralStartupSanityGuard;
    }

    public AdminProceduralSnapshotResponse architecture() {
        return new AdminProceduralSnapshotResponse("architecture", proceduralArchitectureSanityService.report().toMap());
    }

    public AdminProceduralSnapshotResponse legacyBoundary() {
        return new AdminProceduralSnapshotResponse("legacy-boundary", proceduralLegacyBoundaryAuditService.report().toMap());
    }

    public AdminProceduralSnapshotResponse bootstrap() {
        return new AdminProceduralSnapshotResponse("bootstrap", proceduralStartupSanityGuard.snapshot());
    }
}
