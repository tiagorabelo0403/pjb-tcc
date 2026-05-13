package com.tcc.pjb.backend.service.admin.surface;

import com.tcc.pjb.backend.integration.judicial.JudicialConnectorGovernanceService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorPolicyCommand;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorPolicyService;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import org.springframework.stereotype.Service;

@Service
public class AdminJudicialConnectorSurfaceFacadeService {

    private final JudicialConnectorGovernanceService governanceService;
    private final JudicialConnectorPolicyService policyService;
    private final SurfaceProjectionSupport projectionSupport;

    public AdminJudicialConnectorSurfaceFacadeService(JudicialConnectorGovernanceService governanceService,
                                                      JudicialConnectorPolicyService policyService,
                                                      SurfaceProjectionSupport projectionSupport) {
        this.governanceService = governanceService;
        this.policyService = policyService;
        this.projectionSupport = projectionSupport;
    }

    public SurfaceSnapshotResponse governanceNational() {
        return projectionSupport.snapshot("admin-connector-governance", governanceService.report().toMap());
    }

    public SurfaceSnapshotResponse governanceTribunal(String tribunalCodigo) {
        return projectionSupport.snapshot("admin-connector-governance", governanceService.reportForTribunal(tribunalCodigo).toMap());
    }

    public SurfaceSnapshotResponse policyReport() {
        return projectionSupport.snapshot("admin-connector-policy", policyService.report().toMap());
    }

    public SurfaceSnapshotResponse policyUpsert(JudicialConnectorPolicyCommand command) {
        return projectionSupport.snapshot("admin-connector-policy", policyService.save(command).toMap());
    }
}
